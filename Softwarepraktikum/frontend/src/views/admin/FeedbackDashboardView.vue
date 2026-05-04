<!--FeedbackDashboardView-->
<script setup lang="ts">
import {ref, onMounted, computed, watch} from 'vue';
import { useRouter } from 'vue-router';
import { Button } from "agnostic-vue";
import { Toast, showToast } from "@/ts/toasts";
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons';
import axios from 'axios';
import config from "@/config";

// --- Event Listener für Authentifizierung ---
window.addEventListener('storage', (e) => {
    if (e.key === 'employeeId') {
        // Redirect zur Login-Seite
        router.push('/');
    }
});

// --- Interfaces ---
/**
 * Definiert die Struktur einer Feedback-Nachricht
 */
interface FeedbackMessage {
    id: number;
    message: string;
    timestamp: string;
    isAdminMessage: boolean;
    isAnonymous: boolean;
    anonymousAuthorId?: number;
    employee?: {
        id: number;
        firstName: string;
        lastName: string;
    };
}

/**
 * Definiert die Struktur eines Mitarbeiter-Objekts
 */
interface Employee {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    admin: boolean;
}

/**
 * Definiert die Struktur der Feedback-Daten
 */
interface FeedbackData {
    id: number;
    workshopID: number;
    employee?: {
        id: number;
        firstName: string;
        lastName: string;
        email: string;
    };
    rating: number;
    comment: string;
    anonymous: boolean;
    anonymousToken: string | null;
    timestamp: string;
    messages: FeedbackMessage[];
}

// --- State Management ---
const router = useRouter();
const isLoading = ref(true);

// Feedback und Workshop Zustand
const feedbackList = ref<FeedbackData[]>([]);
const selectedWorkshop = ref<number | 'all'>('all');
const workshops = ref<{id: number, title: string}[]>([]);

// Modal Zustände
const showContactModal = ref(false);
const showMessagesModal = ref(false);
const showRatingModal = ref(false);
const showTrainerRatingsModal = ref(false);

// Ausgewählte Elemente
const selectedFeedback = ref<FeedbackData | null>(null);
const selectedMessages = ref<FeedbackMessage[]>([]);
const selectedMessageFeedback = ref<FeedbackData | null>(null);
const selectedTrainerForStats = ref<number | null>(null);

// Mitarbeiter und Trainer
const employees = ref<{id: number, firstName: string, lastName: string}[]>([]);
const trainers = ref<Employee[]>([]);

// Nachrichten und Bewertungen
const contactMessage = ref('');
const trainerAverageRating = ref(0);
const trainerStats = ref({
    averageRating: 0,
    totalRatings: 0,
    1: 0, 2: 0, 3: 0, 4: 0, 5: 0
});

// --- API Konfiguration ---
const api = axios.create({
    baseURL: '/api/v1'
});

// --- Berechnete Eigenschaften ---
/**
 * Berechnet den Prozentsatz für eine bestimmte Sternebewertung
 */
function getRatingPercentage(stars: number) {
    const list = filteredFeedbackList.value;
    if (list.length === 0) return 0;
    const count = list.filter(f => Math.round(f.rating) === stars).length;
    return Math.round((count / list.length) * 100);
}

/**
 * Berechnet die durchschnittliche Bewertung
 */
const averageRating = computed(() => {
    const list = filteredFeedbackList.value;
    if (list.length === 0) return 0;
    const sum = list.reduce((acc, f) => acc + f.rating, 0);
    return (sum / list.length).toFixed(1);
});

/**
 * Berechnet die Gesamtzahl der Feedbacks
 */
const totalFeedback = computed(() => {
    return filteredFeedbackList.value.length;
});

/**
 * Berechnet den Prozentsatz anonymer Feedbacks
 */
const anonymousPercentage = computed(() => {
    const list = filteredFeedbackList.value;
    if (list.length === 0) return 0;
    const anonymous = list.filter(f => f.anonymous).length;
    return ((anonymous / list.length) * 100).toFixed(1);
});

/**
 * Berechnet die durchschnittliche Trainerbewertung
 */
const selectedTrainerRating = computed(() => {
    if (selectedWorkshop.value === 'all') {
        return trainerAverageRating.value.toFixed(1);
    }
    const workshop = workshops.value.find(w => w.id === selectedWorkshop.value);
    if (!workshop?.trainer) return "0.0";

    const trainerFeedbacks = feedbackList.value.filter(f =>
        f.workshopID === selectedWorkshop.value && f.trainerRating
    );
    if (!trainerFeedbacks.length) return "0.0";
    const avg = trainerFeedbacks.reduce((sum, f) => sum + f.trainerRating, 0) / trainerFeedbacks.length;
    return avg.toFixed(1);
});

/**
 * Gibt den Namen des ausgewählten Trainers zurück
 */
const selectedTrainerName = computed(() => {
    if (selectedWorkshop.value === 'all') return 'Alle Trainer';
    const workshop = workshops.value.find(w => w.id === selectedWorkshop.value);
    return workshop?.trainer ?
        `${workshop.trainer.firstName} ${workshop.trainer.lastName}` :
        'Kein Trainer';
});

// --- API Funktionen ---
/**
 * Lädt die Mitarbeiterdaten vom Server
 */
async function loadEmployees() {
    try {
        const response = await api.get(`${config.API_BASE_URL}/employees`);
        employees.value = response.data;
    } catch (err) {
        console.error('Fehler beim Laden der Mitarbeiterdaten:', err);
    }
}

/**
 * Lädt alle benötigten Daten vom Server
 * (Workshops, Feedback, Mitarbeiter)
 */
async function loadData() {
    try {
        isLoading.value = true;
        const [workshopsResponse, feedbackResponse, employeesResponse] = await Promise.all([
            api.get('/workshops'),
            api.get('/feedback'),
            api.get('/employees')
        ]);

        workshops.value = workshopsResponse.data;
        feedbackList.value = feedbackResponse.data;
        employees.value = employeesResponse.data;
        trainers.value = employeesResponse.data.filter(emp => emp.admin);

        // Durchschnittliche Bewertung für alle Trainer
        const trainerFeedbacks = feedbackList.value.filter(f => f.trainerRating);
        if (trainerFeedbacks.length > 0) {
            const avgRating = trainerFeedbacks.reduce((sum, f) => sum + f.trainerRating, 0) / trainerFeedbacks.length;
            trainerAverageRating.value = avgRating;
        }
    } catch (err) {
        console.error('Error loading data:', err);
        showToast(new Toast("Fehler", "Daten konnten nicht geladen werden", "error", faExclamation));
    } finally {
        isLoading.value = false;
    }
}

/**
 * Löscht ein Feedback nach Bestätigung
 */
async function deleteFeedback(id: number) {
    if (!confirm('Möchten Sie dieses Feedback wirklich löschen?')) return;

    try {
        await api.delete(`/feedback/${id}`);
        showToast(new Toast(
            "Erfolg",
            "Feedback wurde gelöscht",
            "success",
            faCheck
        ));
        await loadData();
    } catch (err) {
        showToast(new Toast(
            "Fehler",
            "Feedback konnte nicht gelöscht werden",
            "error",
            faExclamation
        ));
    }
}

/**
 * Löscht eine einzelne Nachricht nach Bestätigung
 */
async function deleteMessage(messageId: number) {
    if (!confirm('Möchten Sie diese Nachricht wirklich löschen?')) return;

    try {
        await api.delete(`/feedback/messages/${messageId}`);

        // UI Update
        if (selectedFeedback.value && selectedFeedback.value.messages) {
            selectedFeedback.value.messages = selectedFeedback.value.messages.filter(
                msg => msg.id !== messageId
            );
            selectedMessages.value = selectedFeedback.value.messages;
        }

        // Liste aktualisieren
        feedbackList.value = feedbackList.value.map(feedback => {
            if (feedback.id === selectedFeedback.value?.id) {
                return {
                    ...feedback,
                    messages: feedback.messages.filter(msg => msg.id !== messageId)
                };
            }
            return feedback;
        });

        showToast(new Toast(
            "Erfolg",
            "Nachricht wurde gelöscht",
            "success",
            faCheck
        ));

        loadData().catch(err => {
            console.error('Error reloading data:', err);
        });

    } catch (err) {
        console.error('Error deleting message:', err);
        showToast(new Toast(
            "Fehler",
            "Nachricht konnte nicht gelöscht werden",
            "error",
            faExclamation
        ));
        await loadData();
    }
}

// --- Nachrichten Funktionen ---
/**
 * Prüft ob eine Nachricht vom eingeloggten Benutzer stammt
 */
function isOwnMessage(message: FeedbackMessage): boolean {
    const employeeId = parseInt(localStorage.getItem('employeeId') || '0');
    return message.employee?.id === employeeId;
}

/**
 * Prüft ob der Benutzer die Berechtigung zum Löschen einer Nachricht hat
 */
function canDeleteMessage(message: FeedbackMessage): boolean {
    const employeeId = parseInt(localStorage.getItem('employeeId') || '0');
    const currentEmployee = employees.value.find(e => e.id === employeeId);
    return currentEmployee?.admin === true;
}

// --- Trainer Statistiken ---
/**
 * Lädt die Statistiken für einen ausgewählten Trainer
 */
async function loadSelectedTrainerStats() {
    if (!selectedTrainerForStats.value) return;
    try {
        const response = await api.get(`/feedback/trainer/${selectedTrainerForStats.value}/stats`);
        trainerStats.value = {
            ...response.data,
            ...calculateRatingDistribution(selectedTrainerForStats.value)
        };
    } catch (err) {
        console.error('Error loading trainer stats:', err);
        showToast(new Toast("Fehler", "Trainer-Statistiken konnten nicht geladen werden", "error", faExclamation));
    }
}

/**
 * Berechnet die Verteilung der Bewertungen für einen Trainer
 */
function calculateRatingDistribution(trainerId: number) {
    const trainerFeedbacks = feedbackList.value.filter(f =>
        f.workshopID &&
        workshops.value.find(w => w.id === f.workshopID && w.trainer?.id === trainerId)
    );

    const total = trainerFeedbacks.length;
    const distribution = {1: 0, 2: 0, 3: 0, 4: 0, 5: 0};

    trainerFeedbacks.forEach(f => {
        const rating = Math.round(f.trainerRating);
        if (rating >= 1 && rating <= 5) {
            distribution[rating]++;
        }
    });

    return Object.fromEntries(
        Object.entries(distribution).map(([rating, count]) =>
            [rating, Math.round((count / total) * 100) || 0]
        )
    );
}

// --- Chat Funktionen ---
/**
 * Öffnet den Nachrichten-Modal für ein bestimmtes Feedback
 */
function openMessagesModal(feedback: FeedbackData) {
    selectedMessages.value = feedback.messages;
    selectedFeedback.value = feedback;
    showMessagesModal.value = true;
}

/**
 * Sendet eine neue Nachricht
 */
async function sendMessage() {
    if (!selectedFeedback.value || !contactMessage.value.trim()) return;

    try {
        const employeeId = localStorage.getItem('employeeId');
        if (!employeeId) {
            throw new Error('Nicht eingeloggt');
        }

        const currentEmployee = employees.value.find(e => e.id === parseInt(employeeId));
        if (!currentEmployee) {
            throw new Error('Mitarbeiter nicht gefunden');
        }

        const url = `/feedback/${selectedFeedback.value.id}/messages`;
        const response = await api.post(
            url,
            contactMessage.value,
            {
                params: {
                    employeeId: employeeId,
                    isAdminMessage: currentEmployee.admin,
                    anonymous: false
                },
                headers: {
                    'Content-Type': 'text/plain'
                }
            }
        );

        await loadData();
        const updatedFeedback = feedbackList.value.find(f => f.id === selectedFeedback.value?.id);
        if (updatedFeedback) {
            selectedFeedback.value = updatedFeedback;
        }

        contactMessage.value = '';
        showToast(new Toast(
            "Erfolg",
            "Nachricht wurde gesendet",
            "success",
            faCheck
        ));
    } catch (err) {
        console.error('Error sending message:', err);
        showToast(new Toast(
            "Fehler",
            "Nachricht konnte nicht gesendet werden",
            "error",
            faExclamation
        ));
    }
}

// --- Hilfsfunktionen ---
/**
 * Gibt den Namen eines Mitarbeiters zurück
 */
function getEmployeeName(employeeId: number) {
    const employee = employees.value.find(e => e.id === employeeId);
    return employee ? `${employee.firstName} ${employee.lastName}` : 'Unbekannt';
}

// --- Computed Properties & Watchers ---
/**
 * Gefilterte Liste der Feedbacks basierend auf ausgewähltem Workshop
 */
const filteredFeedbackList = computed(() => {
    if (selectedWorkshop.value === 'all') {
        return feedbackList.value;
    }
    return feedbackList.value.filter(feedback => feedback.workshopID === selectedWorkshop.value);
});

/**
 * Beobachtet Änderungen am ausgewählten Workshop
 * Lädt entsprechende Trainer-Statistiken
 */
watch(selectedWorkshop, async (newWorkshopId) => {
    if (newWorkshopId !== 'all') {
        const workshop = workshops.value.find(w => w.id === newWorkshopId);
        if (workshop?.trainer) {
            selectedTrainerForStats.value = workshop.trainer.id;
            await loadSelectedTrainerStats();
        }
    }
});

// --- Lifecycle Hooks ---
onMounted(loadData);
</script>

<template>
    <main class="feedback-container">
        <!-- Header-Bereich mit Filterung und Navigation -->
        <div class="header">
            <h1>Feedback Dashboard</h1>
            <div class="filter-section">
                <select v-model="selectedWorkshop" class="workshop-filter">
                    <option value="all">Alle Workshops</option>
                    <option v-for="workshop in workshops"
                            :key="workshop.id"
                            :value="workshop.id">
                        {{ workshop.title }}
                    </option>
                </select>
            </div>
            <div class="back-button">
                <Button class="back-button-blue" @click="router.push('/admin')">Zurück</Button>
            </div>
        </div>

        <!-- Bewertungs-Modal -->
        <div v-if="showRatingModal" class="modal rating-modal">
            <div class="modal-content rating-details">
                <div class="modal-header">
                    <button class="close-button" @click="showRatingModal = false">&times;</button>
                    <h3>Detaillierte Bewertungen</h3>
                </div>

                <div class="rating-overview">
                    <div class="overall-rating">
                        <div class="big-rating">{{ averageRating }}/5</div>
                        <div class="total-ratings">{{ totalFeedback }} Bewertungen</div>
                    </div>

                    <div class="rating-bars">
                        <div v-for="stars in 5" :key="stars" class="rating-bar-row">
                            <span class="stars-label">{{ stars }} Sterne</span>
                            <div class="rating-bar-container">
                                <div class="rating-bar" :style="{ width: getRatingPercentage(stars) + '%' }"></div>
                            </div>
                            <span class="rating-percentage">{{ getRatingPercentage(stars) }}%</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Trainer-Bewertungs-Modal -->
        <div v-if="showTrainerRatingsModal" class="modal trainer-ratings-modal-wrapper">
            <div class="modal-content rating-details">
                <div class="modal-header">
                    <button class="close-button" @click="showTrainerRatingsModal = false">&times;</button>
                    <h3>Detaillierte Trainer-Bewertungen</h3>
                </div>

                <select v-model="selectedTrainerForStats" @change="loadSelectedTrainerStats" class="trainer-select">
                    <option value="">Trainer auswählen</option>
                    <option v-for="trainer in trainers" :key="trainer.id" :value="trainer.id">
                        {{ trainer.firstName }} {{ trainer.lastName }}
                    </option>
                </select>

                <div class="rating-overview">
                    <div class="overall-rating">
                        <div class="big-rating">{{ trainerStats.averageRating.toFixed(1) }}/5</div>
                        <div class="total-ratings">{{ trainerStats.totalRatings }} Bewertungen</div>
                    </div>

                    <div class="rating-bars">
                        <div v-for="stars in 5" :key="stars" class="rating-bar-row">
                            <span class="stars-label">{{ stars }} Sterne</span>
                            <div class="rating-bar-container">
                                <div class="rating-bar" :style="{ width: trainerStats[stars] + '%' }"></div>
                            </div>
                            <span class="rating-percentage">{{ trainerStats[stars] || 0 }}%</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Hauptinhalt -->
        <div class="content">
            <!-- Statistik-Karten -->
            <div class="stats-grid">
                <div class="stat-card clickable rating-card" @click="showRatingModal = true">
                    <h3>Durchschnittliche Workshop Bewertung:</h3>
                    <div class="stat-value">{{ averageRating }}/5</div>
                    <div class="stars">★★★★★</div>
                </div>
                <div class="stat-card clickable trainer-rating-card" @click="showTrainerRatingsModal = true">
                    <h3>Trainer Bewertungen:</h3>
                    <h3>{{ selectedTrainerName }}</h3>
                    <div class="stat-value">{{ selectedTrainerRating }}/5</div>
                    <div class="stars">★★★★★</div>
                    <div class="click-hint">Klicken für Trainer Details</div>
                </div>
                <div class="stat-card">
                    <h3>Anonymitätsrate</h3>
                    <div class="stat-value">{{ anonymousPercentage }}%</div>
                </div>
            </div>

            <div class="feedback-count">Anzahl Feedbacks: {{ totalFeedback }}</div>

            <!-- Feedback-Tabelle -->
            <div class="feedback-table">
                <table>
                    <thead>
                    <tr>
                        <th>Workshop ID</th>
                        <th>Workshop</th>
                        <th>Bewertung</th>
                        <th>Trainer Bewertung</th>
                        <th>Trainer</th>
                        <th>Kommentar</th>
                        <th>Kommentar Trainer</th>
                        <th>Absender</th>
                        <th>Datum</th>
                        <th>Nachrichten</th>
                        <th>Aktionen</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="feedback in filteredFeedbackList" :key="feedback.id">
                        <td>{{ feedback.workshopID }}</td>
                        <td>{{ workshops.find(w => w.id === feedback.workshopID)?.title || feedback.workshopID }}</td>
                        <td>
                            <div class="rating-with-star">
                                {{ feedback.rating }}/5
                            </div>
                        </td>
                        <td>
                            <div class="rating-with-star">
                                {{ feedback.trainerRating }}/5
                            </div>
                        </td>
                        <td>{{ workshops.find(w => w.id === feedback.workshopID)?.trainer?.firstName }}
                            {{ workshops.find(w => w.id === feedback.workshopID)?.trainer?.lastName }}</td>
                        <td class="comment-cell">{{ feedback.comment }}</td>
                        <td class="comment-cell">{{ feedback.trainerComment }}</td>
                        <td>
                            <template v-if="feedback.anonymous">
                                <span class="anonymous-tag">Anonym</span>
                            </template>
                            <template v-else-if="feedback.employeeID != null">
                                {{ getEmployeeName(feedback.employeeID) }}
                            </template>
                            <template v-else>
                                <span>Unbekannt</span>
                            </template>
                        </td>
                        <td>{{ new Date(feedback.timestamp).toLocaleDateString('de-DE') }}</td>
                        <td>
                            <div v-if="feedback.messages?.length"
                                 class="message-count clickable"
                                 @click="openMessagesModal(feedback)">
                                {{ feedback.messages.length }} Nachricht(en)
                            </div>
                            <div v-else>-</div>
                        </td>
                        <td>
                            <div class="action-buttons">
                                <button class="message-button"
                                        @click="openMessagesModal(feedback)">
                                    {{ feedback.messages?.length ? 'Weitere Nachricht' : 'Antworten' }}
                                </button>
                                <button class="message-button delete"
                                        @click="() => deleteFeedback(feedback.id)">
                                    Löschen
                                </button>
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Chat-Modal für Nachrichten -->
        <div v-if="showMessagesModal" class="modal">
            <div class="modal-content chat-modal">
                <!-- Chat Header -->
                <div class="chat-header">
                    <div class="chat-info">
                        <h3>{{ workshops.find(w => w.id === selectedFeedback?.workshopID)?.title }}</h3>
                        <div class="participant-info">
                            <template v-if="selectedFeedback?.anonymous">
                                <span class="anonymous-tag">Anonym</span>
                            </template>
                            <template v-else-if="selectedFeedback?.employeeID">
                                {{ getEmployeeName(selectedFeedback.employeeID) }}
                            </template>
                            <template v-else>
                                <span>Unbekannt</span>
                            </template>
                            <div class="participant-info2">Diskussionsforum</div>
                        </div>
                    </div>
                    <button class="close-button"
                            @click="showMessagesModal = false">&times;</button>
                </div>

                <!-- Chat Body -->
                <div class="chat-body">
                    <div class="chat-messages">
                        <div v-for="message in selectedFeedback?.messages"
                             :key="message.id"
                             :class="['chat-bubble',
                                    isOwnMessage(message) ? 'own-message' : 'other-message']">
                            <!-- Absender -->
                            <div class="message-sender">
                                {{ message.isAdminMessage ?
                                (message.employee ? `${message.employee.firstName} ${message.employee.lastName} (Admin)` : 'Admin') :
                                (message.isAnonymous ? 'Anonym' :
                                    (message.employee ? `${message.employee.firstName} ${message.employee.lastName}` :
                                        'Anonym'))
                                }}
                            </div>
                            <!-- Nachrichteninhalt -->
                            <div class="message-content">{{ message.message }}</div>
                            <!-- Zeit und Datum -->
                            <div class="message-meta">
                                {{ new Date(message.timestamp).toLocaleString('de-DE', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit',
                                hour12: false
                            }) }}
                            </div>
                            <!-- Lösch-Button für Admin -->
                            <button
                                @click="deleteMessage(message.id)"
                                class="delete-message-btn">
                                ×
                            </button>
                        </div>
                    </div>

                    <!-- Chat Input -->
                    <div class="chat-input">
                        <div class="input-group">
                            <textarea
                                v-model="contactMessage"
                                placeholder="Nachricht eingeben..."
                                class="message-input"
                                @keyup.enter="sendMessage"
                            ></textarea>
                        </div>
                        <Button class="send-button" @click="sendMessage">
                            Senden
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    </main>
</template>

<style scoped>
/* --- Basis Layout Styles --- */
html, body {
    margin: 0;
    padding: 0;
    width: 100vw;
    height: 100vh;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    overflow-x: hidden;
}

.feedback-container {
    padding: 4rem;
    max-width: 9000px;
    margin: 0 auto;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    color: #000;
    background-color: #fff;
}

/* --- Header Styles --- */
.header {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
    color: #000;
}

.filter-section {
    padding: 1rem;
    display: flex;
    align-items: center;
    color: #000;
}

.workshop-filter {
    padding: 0.5rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    font-size: 1rem;
    color: #000;
    background-color: #ffffff;
}

/* --- Stats Grid Styles --- */
.stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1.5rem;
    margin-bottom: 2rem;
    color: #000;
}

.stat-card {
    background: #f9f9f9;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    text-align: center;
    color: #000;
}

.stat-value {
    font-size: 2rem;
    font-weight: bold;
    color: #2c3e50;
    margin-top: 0.5rem;
}

/* --- Content Layout --- */
.content {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    padding: 1.5rem;
    margin-top: 2rem;
}

.feedback-count {
    font-size: 1.2rem;
    color: #2c3e50;
    margin-bottom: 1rem;
    padding-left: 0.5rem;
}

/* --- Button Styles --- */
:deep(.back-button-blue) {
    padding: 0.75rem 1.5rem;
    background-color: #3498db !important;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

:deep(.back-button-blue:hover) {
    background-color: #2980b9 !important;
}

/* --- Modal Base Styles --- */
.modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}

.modal-content {
    background: white;
    padding: 2rem;
    border-radius: 12px;
    width: 90%;
    max-width: 500px;
    position: relative;
    z-index: 1001;
}

.close-button {
    padding: 0.5rem;
    background-color: #e74c3c;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1.2rem;
    font-weight: 500;
    min-width: 44px;
    text-align: center;
    transition: background-color 0.2s ease;
}

.close-button:hover {
    background-color: #c0392b;
}

/* --- Table Styles --- */
.feedback-table {
    width: 100%;
    overflow-x: auto;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

table {
    width: 100%;
    border-collapse: collapse;
    min-width: 1200px;
}

th {
    position: sticky;
    top: 0;
    background-color: #f2f2f2;
    z-index: 2;
    white-space: nowrap;
    padding: 1rem;
}

td {
    padding: 1rem;
    vertical-align: top;
    border-bottom: 1px solid #eee;
}

/* Spaltenbreiten */
td:nth-child(1),
td:nth-child(3),
td:nth-child(4) {
    width: 100px;
    white-space: nowrap;
}

td:nth-child(2),
td:nth-child(5),
td:nth-child(8) {
    width: 150px;
    white-space: nowrap;
}

td:nth-child(6),
td:nth-child(7) {
    min-width: 200px;
    max-width: 300px;
    word-wrap: break-word;
}

td:nth-child(9) {
    width: 120px;
    white-space: nowrap;
}

td:nth-child(10),
td:nth-child(11) {
    width: 150px;
    white-space: nowrap;
}

.comment-cell {
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: normal;
    line-height: 1.4;
}

/* --- Rating Styles --- */
.rating-modal .modal-content {
    max-width: 600px;
}

.rating-details {
    padding: 2rem;
}

.rating-overview {
    display: flex;
    gap: 2rem;
    margin-top: 1rem;
}

.overall-rating {
    min-width: 150px;
    text-align: center;
}

.big-rating {
    font-size: 3rem;
    font-weight: bold;
    color: #2c3e50;
}

.total-ratings {
    color: #666;
    font-size: 0.9rem;
}

.rating-bars {
    flex-grow: 1;
}

.rating-bar-row {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 0.5rem;
}

.stars-label {
    min-width: 80px;
}

.rating-bar-container {
    flex-grow: 1;
    background: #eee;
    height: 20px;
    border-radius: 10px;
    overflow: hidden;
}

.rating-bar {
    height: 100%;
    background: #f39c12;
    transition: width 0.3s ease;
}

.rating-percentage {
    min-width: 50px;
}

/* Rating Card Styles */
.rating-card,
.trainer-rating-card {
    position: relative;
}

.rating-card .stars,
.trainer-rating-card .stars {
    color: #f1c40f;
    font-size: 1.5rem;
    margin-top: 0.5rem;
    position: relative;
    display: inline-block;
}

.rating-card .stars::before,
.trainer-rating-card .stars::before {
    content: "★★★★★";
    position: absolute;
    left: 0;
    color: #ddd;
}

.rating-card .stars::after {
    content: "★★★★★";
    position: absolute;
    left: 0;
    color: #f1c40f;
    width: v-bind('(parseFloat(averageRating) / 5 * 100) + "%"');
    overflow: hidden;
}

.trainer-rating-card .stars::after {
    content: "★★★★★";
    position: absolute;
    left: 0;
    color: #f1c40f;
    width: v-bind('(parseFloat(selectedTrainerRating) / 5 * 100) + "%"');
    overflow: hidden;
}

/* --- Action Button Styles --- */
.action-buttons {
    display: flex;
    gap: 0.5rem;
}

.message-button {
    padding: 0.5rem 1rem;
    background-color: #3498db;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

.message-button:hover {
    background-color: #2980b9;
}

.message-button.delete {
    background-color: #e74c3c;
}

.message-button.delete:hover {
    background-color: #c0392b;
}

/* --- Chat Modal Styles --- */
.chat-modal {
    max-width: 800px !important;
    height: 85vh;
    display: flex;
    flex-direction: column;
    padding: 0 !important;
    background: #ffffff;
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.chat-header {
    background-color: #ffffff;
    padding: 1.25rem 1.75rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e2e8f0;
    border-radius: 12px 12px 0 0;
}

.chat-info {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.chat-info h3 {
    font-size: 1.25rem;
    font-weight: 600;
    color: #1a202c;
    margin: 0;
}

.participant-info,
.participant-info2 {
    font-size: 0.9rem;
    color: #64748b;
}

.participant-info2 {
    padding: 0.5rem;
}

/* Chat Body Styles */
.chat-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
    background-color: #f0f2f5;
}

/* Message Bubble Styles */
.chat-bubble {
    max-width: 70%;
    padding: 1rem;
    border-radius: 12px;
    position: relative;
    margin-bottom: 1rem;
}

.own-message {
    background-color: #e3f2fd;
    border: 1px solid #e2e8f0;
    align-self: flex-end;
    margin-left: auto;
    margin-right: 0;
}

.other-message {
    background-color: #ffffff;
    border: 1px solid #e2e8f0;
    align-self: flex-start;
    margin-right: auto;
    margin-left: 0;
}

.message-sender {
    font-weight: 600;
    color: #2980b9;
    margin-bottom: 0.4rem;
}

.message-content {
    margin-bottom: 0.4rem;
    line-height: 1.4;
    word-wrap: break-word;
    overflow-wrap: break-word;
    white-space: pre-wrap;
    max-width: 100%;
}

.message-meta {
    font-size: 0.75rem;
    color: #64748b;
    text-align: left;
}

/* Chat Input Styles */
.chat-input {
    background-color: #ffffff;
    padding: 1.25rem;
    display: flex;
    gap: 1rem;
    align-items: end;
    border-top: 1px solid #e2e8f0;
    border-radius: 0 0 12px 12px;
}

.input-group {
    display: flex;
    flex-direction: column;
    flex: 1;
    gap: 0.5rem;
}

.message-input {
    width: 100%;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 0.8rem;
    min-height: 45px;
    max-height: 120px;
    resize: vertical;
    background-color: #ffffff;
}

.message-input:focus {
    outline: none;
    border-color: #3498db;
}

/* --- Utility Styles --- */
.anonymous-tag {
    background-color: #f39c12;
    color: white;
    padding: 0.2rem 0.5rem;
    border-radius: 4px;
    font-size: 0.9rem;
}

.message-count.clickable {
    cursor: pointer;
    color: #3498db;
    transition: color 0.2s ease;
}

.message-count.clickable:hover {
    color: #2980b9;
    text-decoration: underline;
}

.stat-card.clickable {
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card.clickable:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
    z-index: 1;
}

.click-hint {
    font-size: 0.8rem;
    color: #666;
    margin-top: 0.5rem;
    opacity: 0;
    transition: opacity 0.2s ease;
}

.rating-card:hover .click-hint,
.trainer-rating-card:hover .click-hint {
    opacity: 1;
}

/* --- Delete Button Styles --- */
.delete-message-btn {
    position: absolute;
    top: 0.3rem;
    right: 0.3rem;
    background: none;
    border: none;
    color: #666;
    font-size: 1.2rem;
    cursor: pointer;
    padding: 0.2rem 0.5rem;
    border-radius: 50%;
    opacity: 0;
    transition: opacity 0.2s ease;
}

.chat-bubble:hover .delete-message-btn {
    opacity: 1;
}

.delete-message-btn:hover {
    background-color: rgba(0, 0, 0, 0.1);
    color: #333;
}

/* --- Send Button Styles --- */
.send-button {
    background-color: #27ae60;
    color: white;
    border: none;
    padding: 0.8rem 1.5rem;
    border-radius: 8px;
    cursor: pointer;
    height: 45px;
    font-size: 1rem;
    font-weight: 500;
}

.send-button:hover {
    background-color: #219a52;
}

/* --- Trainer Selection Styles --- */
.trainer-select {
    width: 100%;
    padding: 0.75rem;
    margin: 1rem 0;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    font-size: 1rem;
    background-color: #fff;
}

.trainer-ratings-modal-wrapper {
    z-index: 1001;
}

/* --- Rating Display Styles --- */
td .rating-with-star {
    display: flex;
    align-items: center;
    gap: 0.25rem;
}

td .rating-with-star::after {
    content: "★";
    color: #f1c40f;
    font-size: 1rem;
}

/* --- Responsive Design --- */
@media (max-width: 768px) {
    .feedback-container {
        padding: 1rem;
    }

    .stats-grid {
        grid-template-columns: 1fr;
    }

    .form-header {
        flex-direction: column;
        gap: 1rem;
    }

    .rating-modal .modal-content {
        margin: 0 1rem;
    }

    .chat-modal {
        height: 100vh;
        width: 100vw;
        max-width: none !important;
        border-radius: 0;
    }
}
</style>
