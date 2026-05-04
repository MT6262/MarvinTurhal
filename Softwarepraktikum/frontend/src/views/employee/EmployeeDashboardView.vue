<!-- EmployeeDashboardView.vue -->
<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Button } from "agnostic-vue";
import { Toast, showToast } from "@/ts/toasts";
import {faCheck, faExclamation} from '@fortawesome/free-solid-svg-icons';
import axios from 'axios';
import config from "@/config";
import { ref, onMounted, computed, watch } from 'vue';


window.addEventListener('storage', (e) => {
    if (e.key === 'employeeId') {
        loadData();
    }
});

interface Employee {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    admin: boolean;
}

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

const router = useRouter();
const feedbackList = ref<FeedbackData[]>([]);
const isLoading = ref(true);
const selectedWorkshop = ref<number | 'all'>('all');
const workshops = ref<{id: number, title: string}[]>([]);
const employees = ref<{id: number, firstName: string, lastName: string}[]>([]);
const showRatingModal = ref(false);
const showMessagesModal = ref(false);
const selectedMessages = ref<FeedbackMessage[]>([]);
const selectedFeedback = ref<FeedbackData | null>(null);
const contactMessage = ref('');
const sendAnonymous = ref(false);
const api = axios.create({
    baseURL: '/api/v1'
});
const showTrainerModal = ref(false);
const selectedTrainer = ref<Employee | null>(null);
const trainerStats = ref({
    averageRating: 0,
    totalRatings: 0,
    1: 0,
    2: 0,
    3: 0,
    4: 0,
    5: 0
});

const showTrainerRatingsModal = ref(false);
const selectedTrainerForStats = ref<number | null>(null);
const trainers = ref<Employee[]>([]);
const trainerAverageRating = ref(0);


// Funktion zum Öffnen des Nachrichten-Modals
function openMessagesModal(feedback: FeedbackData) {
    selectedMessages.value = feedback.messages;
    selectedFeedback.value = feedback;
    showMessagesModal.value = true;
}


function isOwnMessage(message: FeedbackMessage): boolean {
    const currentEmployeeId = parseInt(localStorage.getItem('employeeId') || '0');

    // Admin-Nachrichten sind nie eigene Nachrichten
    if (message.adminMessage) {
        return false;
    }

    // Bei normalen Nachrichten: Prüfe employee ID
    if (!message.anonymous && message.employee?.id === currentEmployeeId) {
        return true;
    }

    // Bei anonymen Nachrichten: Prüfe anonymousAuthorId
    if (message.anonymous && message.anonymousAuthorId === currentEmployeeId) {
        return true;
    }

    return false;
}

async function openTrainerModal(trainer: Employee) {
    try {
        const response = await api.get(`/feedback/trainer/${trainer.id}/stats`);
        trainerStats.value = response.data;
        selectedTrainer.value = trainer;
        showTrainerModal.value = true;
    } catch (err) {
        console.error('Error loading trainer stats:', err);
        showToast(new Toast(
            "Fehler",
            "Trainer-Statistiken konnten nicht geladen werden",
            "error",
            faExclamation
        ));
    }
}

async function sendMessage() {
    if (!selectedFeedback.value || !contactMessage.value.trim()) return;

    try {
        const employeeId = localStorage.getItem('employeeId');
        if (!employeeId) {
            throw new Error('Nicht eingeloggt');
        }

        await api.post(
            `/feedback/${selectedFeedback.value.id}/messages`,
            contactMessage.value,
            {
                params: {
                    employeeId: employeeId,
                    anonymous: sendAnonymous.value
                },
                headers: {
                    'Content-Type': 'text/plain'
                }
            }
        );

        // Daten neu laden und UI aktualisieren
        await loadData();
        const updatedFeedback = feedbackList.value.find(f => f.id === selectedFeedback.value?.id);
        if (updatedFeedback) {
            selectedFeedback.value = updatedFeedback;
            selectedMessages.value = updatedFeedback.messages;
        }

        // UI zurücksetzen
        contactMessage.value = '';
        sendAnonymous.value = false;
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

// Hilfsfunktion zum Anzeigen des Mitarbeiternamens
function getEmployeeName(employeeId: number) {
    const employee = employees.value.find(e => e.id === employeeId);
    return employee ? `${employee.firstName} ${employee.lastName}` : 'Unbekannt';
}

function getRatingPercentage(stars: number) {
    const list = filteredFeedbackList.value;
    if (list.length === 0) return 0;
    const count = list.filter(f => Math.round(f.rating) === stars).length;
    return Math.round((count / list.length) * 100);
}

function getTrainerRatingPercentage(stars: number) {
    if (!selectedTrainerForStats.value || !trainerStats.value) return 0;
    const feedbacks = feedbackList.value.filter(f =>
        f.workshopID &&
        workshops.value.find(w =>
            w.id === f.workshopID &&
            w.trainer?.id === selectedTrainerForStats.value
        )
    );

    if (feedbacks.length === 0) return 0;

    const count = feedbacks.filter(f => Math.round(f.trainerRating) === stars).length;
    return Math.round((count / feedbacks.length) * 100);
}

const averageRating = computed(() => {
    const list = filteredFeedbackList.value;
    if (list.length === 0) return 0;
    const sum = list.reduce((acc, f) => acc + f.rating, 0);
    return (sum / list.length).toFixed(1);
});

const totalFeedback = computed(() => {
    return filteredFeedbackList.value.length;
});

const anonymousPercentage = computed(() => {
    const list = filteredFeedbackList.value;
    if (list.length === 0) return 0;
    const anonymous = list.filter(f => f.anonymous).length;
    return ((anonymous / list.length) * 100).toFixed(1);
});

// Neue Funktion zum Öffnen des Admin-Profils
async function openAdminModal(adminId: number) {
    try {
        // Zuerst den Admin als Employee laden
        const admin = employees.value.find(e => e.id === adminId);
        if (!admin) return;

        // Statistiken laden
        const response = await api.get(`/feedback/trainer/${adminId}/stats`);
        trainerStats.value = response.data;
        selectedTrainer.value = admin;
        showTrainerModal.value = true;
    } catch (err) {
        console.error('Error loading admin stats:', err);
        showToast(new Toast(
            "Fehler",
            "Admin-Statistiken konnten nicht geladen werden",
            "error",
            faExclamation
        ));
    }
}

// Funktion zum Löschen einer Nachricht
async function deleteMessage(messageId: number) {
    if (!confirm('Möchten Sie diese Nachricht wirklich löschen?')) return;

    try {
        // Zuerst vom Server löschen
        await api.delete(`/feedback/messages/${messageId}`);

        // Lade sowohl Feedback als auch Nachrichten neu
        const [feedbackResponse] = await Promise.all([
            api.get('/feedback')
        ]);

        // Aktualisiert die Hauptliste
        feedbackList.value = feedbackResponse.data;

        // Wenn ein Feedback ausgewählt ist, werden die Nachrichten separat neu geladen
        if (selectedFeedback.value) {
            const messagesRes = await api.get(`/feedback/${selectedFeedback.value.id}/messages`);
            selectedMessages.value = messagesRes.data;

            // Aktualisiert auch das ausgewählte Feedback
            const updatedFeedback = feedbackList.value.find(f => f.id === selectedFeedback.value?.id);
            if (updatedFeedback) {
                selectedFeedback.value = updatedFeedback;
            }
        }

        showToast(new Toast(
            "Erfolg",
            "Nachricht wurde gelöscht",
            "success",
            faCheck
        ));
    } catch (err) {
        console.error('Error deleting message:', err);
        if (err instanceof ReferenceError) {
            showToast(new Toast(
                "Erfolg",
                "Nachricht wurde gelöscht",
                "success",
                faCheck
            ));
        } else {
            showToast(new Toast(
                "Fehler",
                "Nachricht konnte nicht gelöscht werden",
                "error",
                faExclamation
            ));
        }
    }
}

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

const selectedTrainerName = computed(() => {
    if (selectedWorkshop.value === 'all') {
        return 'Alle Trainer';
    }
    const workshop = workshops.value.find(w => w.id === selectedWorkshop.value);
    return workshop?.trainer ?
        `${workshop.trainer.firstName} ${workshop.trainer.lastName}` :
        'Kein Trainer';
});

// Funktion zum Öffnen des Modals mit vorausgewähltem Trainer
async function openTrainerRatingsModal() {
    showTrainerRatingsModal.value = true;
    if (selectedWorkshop.value !== 'all') {
        const workshop = workshops.value.find(w => w.id === selectedWorkshop.value);
        if (workshop?.trainer) {
            selectedTrainerForStats.value = workshop.trainer.id;
            await loadSelectedTrainerStats();
        }
    }
}

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

        // Durchschnittliche Bewertung aller Trainer berechnen
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

const filteredFeedbackList = computed(() => {
    if (selectedWorkshop.value === 'all') {
        return feedbackList.value;
    }
    return feedbackList.value.filter(feedback =>
        feedback.workshopID === selectedWorkshop.value
    );
});

// Trainer vorauswählen wenn Workshop ausgewählt wird
watch(selectedWorkshop, async (newWorkshopId) => {
    if (newWorkshopId !== 'all') {
        const workshop = workshops.value.find(w => w.id === newWorkshopId);
        if (workshop?.trainer) {
            selectedTrainerForStats.value = workshop.trainer.id;
            await loadSelectedTrainerStats();
        }
    }
});

onMounted(loadData);
</script>

<template>
    <main class="feedback-container">
        <div class="header">
            <h1>Mein Dashboard</h1>
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
                <Button @click="router.push('/user')">Zurück</Button>
            </div>
        </div>


        <!-- Statistics Modal -->
        <div v-if="showRatingModal" class="modal rating-modal">
            <div class="modal-content rating-details">
                <div class="modal-header">
                    <button class="close-button" @click="showRatingModal = false">×</button>
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

        <div class="content">
            <div class="stats-grid">
                <div class="stat-card clickable rating-card" @click="showRatingModal = true">
                    <h3>Durchschnittliche Workshop Bewertung:</h3>
                    <div class="stat-value">{{ averageRating }}/5</div>
                    <div class="stars">★★★★★</div>
                    <div class="click-hint">Klicken für Details</div>
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
                        <td>{{ feedback.comment }}</td>
                        <td>{{ feedback.trainerComment }}</td>
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
                                 class="message-count"
                                 @click="openMessagesModal(feedback)">
                                {{ feedback.messages.length }} Nachricht(en)
                            </div>
                            <div v-else>-</div>
                        </td>
                        <td>
                            <div>
                                <button class="message-button"
                                        @click="openMessagesModal(feedback)">{{ feedback.messages?.length ? 'Weitere Nachricht' : 'Antworten' }}
                                </button>
                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>

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

        <!-- Trainer Modal -->
        <div v-if="showTrainerModal" class="modal trainer-modal-wrapper">
            <div class="modal-content trainer-modal">
                <div class="modal-header">
                    <button class="close-button" @click="showTrainerModal = false">&times;</button>
                </div>
                <div class="trainer-info" v-if="selectedTrainer">
                    <h4>{{ selectedTrainer.firstName }} {{ selectedTrainer.lastName }}</h4>
                    <div class="trainer-stats">
                        <div class="stat-item">
                            <div class="stat-label">Durchschnittliche Bewertung</div>
                            <div class="stat-value">{{ trainerStats.averageRating.toFixed(1) }}/5</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-label">Anzahl Bewertungen</div>
                            <div class="stat-value">{{ trainerStats.totalRatings }}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Nachrichten Modal -->
        <div v-if="showMessagesModal" class="modal">
            <div class="modal-content chat-modal">
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
                                <span>Anonym</span>
                            </template>
                            <div class="participant-info2">Diskussionsforum</div>
                        </div>
                    </div>
                    <button class="close-button" @click="showMessagesModal = false">&times;</button>
                </div>

                <div class="chat-body">
                    <div class="chat-messages">
                        <div v-for="message in selectedMessages"
                             :key="message.id"
                             :class="['chat-bubble', {
         'own-message': isOwnMessage(message),
         'other-message': !isOwnMessage(message)
     }]">
                            <!-- Nachrichteninhalt -->
                            <div class="message-sender">
                                <template v-if="(message.isAdminMessage || employees.find(e => e.id === message.employee?.id)?.admin) && message.employee">
        <span class="clickable-trainer" @click="openTrainerModal(message.employee)">
            {{ message.employee.firstName }} {{ message.employee.lastName }} (Trainer)
        </span>
                                </template>
                                <template v-else-if="message.isAnonymous || message.anonymous">
                                    Anonym
                                </template>
                                <template v-else-if="message.employee">
                                    {{ message.employee.firstName }} {{ message.employee.lastName }}
                                </template>
                                <template v-else>
                                    Anonym
                                </template>
                            </div>
                            <div class="message-content">{{ message.message }}</div>
                            <!-- Time and Date -->
                            <div class="message-meta">
                                {{ new Date(message.timestamp).toLocaleString('de-DE', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit',
                                hour12: false,
                                timeZone: 'Europe/Berlin'
                            }) }}
                            </div>
                            <!-- Lösch-Button nur für eigene Nachrichten -->
                            <button v-if="isOwnMessage(message)"
                                    @click="deleteMessage(message.id)"
                                    class="delete-message-btn">
                                ×
                            </button>
                        </div>
                    </div>

                    <div class="chat-input">
                        <div class="input-group">
        <textarea
            v-model="contactMessage"
            placeholder="Nachricht eingeben..."
            class="message-input"
            @keyup.enter="sendMessage"
        ></textarea>
                            <div class="anonymous-option">
                                <input
                                    type="checkbox"
                                    id="anonymous-checkbox"
                                    v-model="sendAnonymous"
                                >
                                <label for="anonymous-checkbox">Anonym senden</label>
                            </div>
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
/* Basis Styles */
html, body {
    margin: 0;
    padding: 0;
    min-height: 100vh;  /* Mindesthöhe 100% */
    width: 100%;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    overflow-x: hidden;
}

/* Weiße Content Box */
.content {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    padding: 1.5rem;
    margin-top: 2rem;
}

.feedback-container {
    min-height: 100vh;
    padding: 4rem;
    width: 100%;
    margin: 0 auto;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    color: #000;
    box-sizing: border-box;
}



h1{
    font-size: 2.5rem;
    font-weight: 700;
    color: #2c3e50;
    text-align: center;
    margin-bottom: 0.5rem;
    line-height: 1.2;
}

/* Header Styles */
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

/* Stats Grid */
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
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    text-align: center;
    color: #000;
}

.stat-value {
    font-size: 2rem;
    font-weight: bold;
    color: #2c3e50;
    margin-top: 0.5rem;
}

/* Feedback Table */
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

th, td {
    padding: 1rem;
    text-align: left;
    border-bottom: 1px solid #eee;
    color: #000;
}

th {
    background-color: #f2f2f2;
    color: #000;
}

/* Action Buttons */
.action-buttons {
    display: flex;
    gap: 0.5rem;
}

.action-btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 1rem;
    transition: background-color 0.3s ease;
    color: #fff;
}

.action-btn.contact {
    background-color: #3498db;
}

.action-btn.contact:hover {
    background-color: #2980b9;
}

.action-btn.delete {
    background-color: #e74c3c;
}

.action-btn.delete:hover {
    background-color: #c0392b;
}

/* Rating Card Styles */
.rating-card {
    position: relative;
}

/* Rating Anzeigestile für beide Karten vereinheitlichen */
.rating-card .stars, .trainer-rating-card .stars {
    color: #f1c40f;
    font-size: 1.5rem;
    margin-top: 0.5rem;
    position: relative;
    display: inline-block;
}

.rating-card .stars::before, .trainer-rating-card .stars::before {
    content: "★★★★★";
    position: absolute;
    left: 0;
    color: #ddd;
}

.trainer-rating-card .stars::after {
    content: "★★★★★";
    position: absolute;
    left: 0;
    color: #f1c40f;
    width: v-bind('(parseFloat(selectedTrainerRating) / 5 * 100) + "%"');
    overflow: hidden;
}

.rating-card .click-hint, .trainer-rating-card .click-hint {
    font-size: 0.8rem;
    color: #666;
    margin-top: 0.5rem;
    opacity: 0;
    transition: opacity 0.2s ease;
}

.rating-card:hover .click-hint, .trainer-rating-card:hover .click-hint {
    opacity: 1;
}

/* Basis Modal (für alle Modals) */
.modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0,0,0,0.5);
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
}

/* Chat Modal */
.chat-modal {
    max-width: 800px !important;
    height: 85vh;
    display: flex;
    flex-direction: column;
    padding: 0 !important;
    background: #ffffff;
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    z-index: 1000;
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

.participant-info {
    font-size: 0.9rem;
    color: #64748b;
}

.participant-info2 {
    padding: 0.5rem;
    font-size: 0.9rem;
    color: #64748b;
}

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

.chat-bubble {
    max-width: 70%;
    padding: 1rem;
    border-radius: 12px;
    position: relative;
    margin-bottom: 1rem;
}

.chat-bubble .message-sender {
    font-weight: 600;
    color: #2980b9;
    margin-bottom: 0.4rem;
}

.chat-bubble .message-content {
    margin-bottom: 0.4rem;
    line-height: 1.4;
    word-wrap: break-word;
    overflow-wrap: break-word;
    white-space: pre-wrap;
    max-width: 100%;
}

.chat-bubble .message-meta {
    font-size: 0.75rem;
    color: #64748b;
    text-align: left;
}

/* Klickbare Nachrichtenanzahl in der Tabelle */
.message-count {
    cursor: pointer;
    color: #3498db;
    transition: color 0.2s ease;
    display: inline-block;
}

.message-count:hover {
    color: #2980b9;
    text-decoration: underline;
}

.own-message {
    background-color: #e3f2fd !important;
    border: 1px solid #e2e8f0;
    align-self: flex-end !important;
    margin-left: auto !important;
    margin-right: 0 !important;
}

.other-message {
    background-color: #ffffff;
    border: 1px solid #e2e8f0;
    align-self: flex-start;
    margin-right: auto;
    margin-left: 0;
}

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

.anonymous-option {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.9rem;
    color: #64748b;
}

.anonymous-option input[type="checkbox"] {
    cursor: pointer;
}

.anonymous-option label {
    cursor: pointer;
    user-select: none;
}

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

/* Basis Modal (für Chat) */
.modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0,0,0,0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}

/* Spezielles Styling für das Rating Modal */
.rating-modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0,0,0,0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1001;
}

/* Rating Modal Content */
.rating-modal .modal-content {
    max-width: 600px;
    margin-left: 150px;
    background: white;
    padding: 2rem;
    border-radius: 12px;
}

.trainer-select {
    margin: 1rem 0;
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    font-size: 1rem;
    background-color: #fff;
}

.rating-details {
    min-width: 600px;
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

.clickable {
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.clickable:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}

.anonymous-tag {
    background-color: #f39c12;
    color: white;
    padding: 0.2rem 0.5rem;
    border-radius: 4px;
    font-size: 0.9rem;
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

:deep(.header button) {
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

:deep(.header button:hover) {
    background-color: #2980b9 !important;
}

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

.message-button{
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

.message-button:hover{
    background-color: #2980b9;
}

.clickable-trainer {
    font-weight: 600;
    color: #2980b9;
    margin-bottom: 0.4rem;
    cursor: pointer;
    text-decoration: underline;
}

.clickable-trainer:hover {
    color: #1f5d87;
}

.trainer-modal {
    max-width: 400px;
    z-index: 1001;
}

.trainer-info {
    margin-bottom: 1.5rem;
}

.trainer-info h4 {
    font-size: 1.3rem;
    font-weight: 700;
    color: #2c3e50;
    text-align: center;
    margin-bottom: 0.5rem;
    line-height: 1.2;
}

.trainer-stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
    margin-top: 1rem;
}

.stat-item {
    background: #f8f9fa;
    padding: 1rem;
    border-radius: 8px;
    text-align: center;
}

.stat-label {
    font-size: 0.9rem;
    color: #666;
    margin-bottom: 0.5rem;
}

.stat-value {
    font-size: 1.5rem;
    font-weight: bold;
    color: #2c3e50;
}

.trainer-modal-wrapper {
    z-index: 1001;
}

.feedback-count {
    font-size: 1.2rem;
    color: #2c3e50;
    margin-bottom: 1rem;
    padding-left: 0.5rem;
}

.trainer-ratings-modal {
    max-width: 600px !important;
    background: white;
    padding: 2rem;
    border-radius: 12px;
}

.trainer-ratings-modal select {
    width: 100%;
    padding: 0.75rem;
    margin-bottom: 1.5rem;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 1rem;
}

.trainer-ratings-modal .rating-overview {
    display: flex;
    gap: 2rem;
    margin-top: 1rem;
}

.trainer-ratings-modal .rating-bars {
    flex-grow: 1;
    margin-top: 1.5rem;
}

.rating-card .stars::after {
    content: "★★★★★";
    position: absolute;
    left: 0;
    color: #f1c40f;
    width: v-bind('(parseFloat(averageRating) / 5 * 100) + "%"');
    overflow: hidden;
}
</style>
