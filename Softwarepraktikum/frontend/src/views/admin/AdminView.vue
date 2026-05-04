<!--AdminView.vue-->
<script setup lang="ts">
import {ref, computed, onMounted, nextTick} from 'vue'
import { useRouter } from 'vue-router'
import { Button } from "agnostic-vue"
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import axios from 'axios'
import { Toast, showToast } from "@/ts/toasts"
import config from '@/config'

// --- Interfaces ---
/**
 * Definiert ein Workshop-Objekt
 */
interface Workshop {
    id: number;
    title: string;
    description: string;
    startTime: string;
    endTime: string;
    maxParticipants: number;
    currentParticipants: number;
    location: string;
    date?: Date;
    type?: string;
    trainer?: {
        id: number;
        firstName: string;
        lastName: string;
    };
}

/**
 * Definiert ein Exchange Day-Objekt
 */
interface ExchangeDay {
    id: number;
    title: string;
    description: string;
    startTime: string;
    endTime: string;
    date?: Date;
}

// Router Instance
const router = useRouter()

// Kalender-Zustand
const currentDate = ref(new Date())
const viewType = ref<'month' | 'day'>('month')
const weekDays = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So']
const timeSlots = Array.from({ length: 12 }, (_, i) => `${i + 8}:00`)

// Daten-Zustand
const workshops = ref<Workshop[]>([])
const exchangeDays = ref<ExchangeDay[]>([])
const expandedExchangeDays = ref<number[]>([])

// UI-Zustand
const isLoading = ref(false)
const error = ref<string | null>(null)
const selectedWorkshopId = ref<number | null>(null)
const showQRModal = ref(false)

// --- API Konfiguration ---
const axiosInstance = axios.create({
    baseURL: config.API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

// --- Berechnete Eigenschaften ---
/**
 * Berechnet die maximale Anzahl parallel stattfindender Workshops
 * Wird für das Layout der Tagesansicht verwendet
 */
const maxParallelWorkshops = computed(() => {
    const groups = getOverlappingGroups(filteredDayWorkshops.value);
    return Math.max(...groups.map(group => group.length), 1);
});

/**
 * Filtert Exchange Days für den aktuellen Tag
 */
const currentDayExchangeDays = computed(() => {
    const currentDay = new Date(currentDate.value);
    currentDay.setHours(0, 0, 0, 0);

    return exchangeDays.value.filter(exchangeDay => {
        const startDate = new Date(exchangeDay.startTime);
        const endDate = new Date(exchangeDay.endTime);

        startDate.setHours(0, 0, 0, 0);
        endDate.setHours(23, 59, 59, 999);

        return currentDay >= startDate && currentDay <= endDate;
    });
});

/**
 * Filtert Workshops für die Tagesansicht
 */
const filteredDayWorkshops = computed(() => {
    return workshops.value
        .filter(workshop => {
            if (!workshop.date) return false;
            return (
                workshop.date.getDate() === currentDate.value.getDate() &&
                workshop.date.getMonth() === currentDate.value.getMonth() &&
                workshop.date.getFullYear() === currentDate.value.getFullYear()
            );
        })
        .sort((a, b) => a.startTime.localeCompare(b.startTime));
});

// --- Event-Styling Funktionen ---
/**
 * Berechnet die Positionierung für einen Workshop in der Tagesansicht
 * Basierend auf Start- und Endzeit
 */
function calculateEventStyle(startTime: string, endTime: string): { top: string, height: string, left: string, width: string } {
    const startHour = parseInt(startTime.split(':')[0]);
    const startMinute = parseInt(startTime.split(':')[1]);
    const endHour = parseInt(endTime.split(':')[0]);
    const endMinute = parseInt(endTime.split(':')[1]);

    const adjustedStartHour = Math.max(8, Math.min(19, startHour));
    const adjustedEndHour = Math.max(8, Math.min(19, endHour));

    const startFromEight = (adjustedStartHour - 8) * 60 + startMinute;
    const duration = ((adjustedEndHour - adjustedStartHour) * 60 + (endMinute - startMinute));

    return {
        top: `${(startFromEight / 60) * 60}px`,
        height: `${(duration / 60) * 60}px`,
        left: '0',
        width: '100%'
    };
}

// --- API Funktionen ---
/**
 * Lädt alle Workshops vom Server
 * Verarbeitet die Daten für die Kalenderansicht
 */
async function fetchWorkshops() {
    isLoading.value = true;
    error.value = null;
    try {
        const response = await axiosInstance.get('/workshops');
        if (response.data && Array.isArray(response.data)) {
            workshops.value = response.data.map((workshop: any) => {
                const startDateTime = new Date(workshop.startTime);
                const endDateTime = new Date(workshop.endTime);
                return {
                    ...workshop,
                    date: startDateTime,
                    startTime: startDateTime.toLocaleTimeString('de-DE', {
                        hour: '2-digit',
                        minute: '2-digit'
                    }),
                    endTime: endDateTime.toLocaleTimeString('de-DE', {
                        hour: '2-digit',
                        minute: '2-digit'
                    }),
                    type: 'workshop',
                    currentParticipants: workshop.currentParticipants || 0
                };
            });
        } else {
            workshops.value = [];
            console.error("Unexpected response format:", response.data);
        }
    } catch (err: any) {
        console.error('Error fetching workshops:', err);
        workshops.value = [];
        showToast(new Toast("Fehler", "Workshops konnten nicht geladen werden", "error", faExclamation));
    } finally {
        isLoading.value = false;
    }
}

/**
 * Lädt alle Exchange Days vom Server
 */
async function fetchExchangeDays() {
    try {
        const response = await axiosInstance.get('/exchangedays');
        if (response.data) {
            exchangeDays.value = response.data.map((exchangeDay: any) => ({
                ...exchangeDay,
                date: new Date(exchangeDay.startTime)
            }));
        }
    } catch (err: any) {
        console.error('Fehler beim Laden der Exchange Days:', err);
        showToast(new Toast("Fehler", "Exchange Days konnten nicht geladen werden", "error", faExclamation));
    }
}

// --- CRUD Operationen ---
/**
 * Löscht einen Workshop nach Bestätigung
 */
async function deleteWorkshop(workshopId: number) {
    if (!confirm('Möchten Sie diesen Workshop wirklich löschen?')) return;

    isLoading.value = true;
    try {
        await axiosInstance.delete(`/workshops/${workshopId}`);
        await fetchWorkshops();
        showToast(new Toast("Erfolg", "Workshop wurde gelöscht", "success", faCheck));
    } catch (err) {
        console.error('Error deleting workshop:', err);
        showToast(new Toast("Fehler", "Workshop konnte nicht gelöscht werden", "error", faExclamation));
    } finally {
        isLoading.value = false;
    }
}

/**
 * Löscht einen Exchange Day nach Bestätigung
 */
async function deleteExchangeDay(exchangeDayId: number) {
    if (!confirm('Möchten Sie diesen Exchange Day wirklich löschen?')) return;

    isLoading.value = true;
    try {
        await axiosInstance.delete(`/exchangedays/${exchangeDayId}`);
        await fetchExchangeDays();
        showToast(new Toast("Erfolg", "Exchange Day wurde gelöscht", "success", faCheck));
    } catch (err) {
        console.error('Error deleting exchange day:', err);
        showToast(new Toast("Fehler", "Exchange Day konnte nicht gelöscht werden", "error", faExclamation));
    } finally {
        isLoading.value = false;
    }
}

// --- Navigation Functions ---
function goToCreateWorkshop() {
    router.push('/workshop/create');
}

function goToCreateExchangeday() {
    router.push('/exchangeday/create');
}

function goToWorkshopDetails(workshopId: number) {
    router.push(`/workshop/${workshopId}`);
}

function goToEditWorkshop(workshopId: number) {
    router.push(`/workshop/${workshopId}/edit`);
}

function goToEditExchangeDay(exchangeDayId: number) {
    router.push(`/exchangeday/${exchangeDayId}/edit`);
}

// --- Kalender-Logik ---
/**
 * Generiert die Monatsansicht mit Workshops und Exchange Days
 * Berücksichtigt auch Tage des vorherigen/nächsten Monats
 */
function getMonthWorkshops() {
    const year = currentDate.value.getFullYear();
    const month = currentDate.value.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const days = [];

    // Behandle Tage vor dem ersten Tag des Monats
    const firstDayOfWeek = firstDay.getDay() || 7;
    for (let i = 1; i < firstDayOfWeek; i++) {
        days.push({
            date: new Date(year, month, 1 - (firstDayOfWeek - i)),
            workshops: [],
            exchangeDays: []
        });
    }

    // Verarbeite jeden Tag des Monats
    for (let d = 1; d <= lastDay.getDate(); d++) {
        const day = new Date(year, month, d);
        day.setHours(0, 0, 0, 0);

        const dayWorkshops = workshops.value.filter(workshop => {
            if (!workshop.date) return false;
            return (
                workshop.date.getDate() === day.getDate() &&
                workshop.date.getMonth() === day.getMonth() &&
                workshop.date.getFullYear() === day.getFullYear()
            );
        });

        const dayExchangeDays = exchangeDays.value.filter(exchangeDay => {
            const startDate = new Date(exchangeDay.startTime);
            const endDate = new Date(exchangeDay.endTime);

            startDate.setHours(0, 0, 0, 0);
            endDate.setHours(23, 59, 59, 999);

            return day >= startDate && day <= endDate;
        });

        days.push({
            date: day,
            workshops: dayWorkshops,
            exchangeDays: dayExchangeDays
        });
    }

    return days;
}

// --- Überlappungs-Logik ---
/**
 * Gruppiert überlappende Workshops für die Tagesansicht
 * Wird für die korrekte Darstellung paralleler Workshops verwendet
 */
function getOverlappingGroups(workshops: Workshop[]): Workshop[][] {
    const groups: Workshop[][] = [];
    workshops.sort((a, b) => a.startTime.localeCompare(b.startTime));

    for (const workshop of workshops) {
        let added = false;
        for (const group of groups) {
            const lastWorkshop = group[group.length - 1];
            const workshopStart = new Date(`2000-01-01T${workshop.startTime}`);
            const lastWorkshopEnd = new Date(`2000-01-01T${lastWorkshop.endTime}`);

            if (workshopStart < lastWorkshopEnd) {
                group.push(workshop);
                added = true;
                break;
            }
        }
        if (!added) {
            groups.push([workshop]);
        }
    }

    return groups;
}

// --- Navigation und View Controls ---
function switchToDay(date: Date) {
    currentDate.value = date;
    viewType.value = 'day';
}

function previousDay() {
    currentDate.value = new Date(currentDate.value.getTime() - 24 * 60 * 60 * 1000);
}

function nextDay() {
    currentDate.value = new Date(currentDate.value.getTime() + 24 * 60 * 60 * 1000);
}

function previousMonth() {
    currentDate.value = new Date(
        currentDate.value.getFullYear(),
        currentDate.value.getMonth() - 1
    );
}

function nextMonth() {
    currentDate.value = new Date(
        currentDate.value.getFullYear(),
        currentDate.value.getMonth() + 1
    );
}

function toggleView() {
    viewType.value = viewType.value === 'month' ? 'day' : 'month';
}

// --- UI-Interaktionen ---
/**
 * Öffnet den QR-Code Modal für Workshop-Feedback
 */
function openQRModal(workshopId: number) {
    selectedWorkshopId.value = workshopId;
    showQRModal.value = true;

    nextTick(() => {
        const qrContainer = document.getElementById('qrcode');
        if (qrContainer) {
            qrContainer.innerHTML = '';
            new QRCode(qrContainer, {
                text: `${window.location.origin}/#/workshop-feedback-login/${workshopId}`,
                width: 250,
                height: 250
            });
        }
    });
}

/**
 * Steuert die Expansion/Kollaps von Exchange Days in der Liste
 */
function toggleExchangeDay(id: number) {
    const index = expandedExchangeDays.value.indexOf(id);
    if (index === -1) {
        expandedExchangeDays.value.push(id);
    } else {
        expandedExchangeDays.value.splice(index, 1);
    }
}

// --- Lifecycle Hooks ---
/**
 * Lädt initial alle benötigten Daten
 */
onMounted(async () => {
    await Promise.all([fetchWorkshops(), fetchExchangeDays()]);
});
</script>

<template>
    <!--Hauptcontainer für die Admin-Ansicht-->
    <div class="page-container">
        <div class="calendar">
            <!-- Loading und Error States -->
            <div v-if="isLoading" class="loading-overlay">
                Lädt...
            </div>

            <div v-if="error" class="error-message">
                {{ error }}
            </div>

            <div v-if="!isLoading && workshops.length === 0" class="empty-state">
                <h3>Keine Workshops vorhanden</h3>
                <p>Erstellen Sie Ihren ersten Workshop mit dem "Workshop anlegen" Button.</p>
            </div>

            <!-- Control Panel -->
            <div class="view-controls">
                <div class="button-group">
                    <Button class="create-btn" @click="goToCreateWorkshop">
                        Workshop anlegen
                    </Button>
                    <Button class="create-btn" @click="goToCreateExchangeday">
                        Exchangeday anlegen
                    </Button>
                    <Button class="create-btn" @click="router.push('/admin/feedback')">
                        Feedback Dashboard
                    </Button>
                </div>
                <Button class="toggle-view-btn" @click="toggleView">
                    {{ viewType === 'month' ? 'Tagesansicht' : 'Monatsansicht' }}
                </Button>
            </div>

            <!-- Monatsansicht -->
            <div v-if="viewType === 'month'">
                <div class="calendar-header">
                    <Button class="nav-btn" @click="previousMonth">&lt;</Button>
                    <h2>{{ currentDate.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' }) }}</h2>
                    <Button class="nav-btn" @click="nextMonth">&gt;</Button>
                </div>

                <div class="calendar-grid">
                    <!-- Wochentage Header -->
                    <div v-for="day in weekDays" :key="day" class="weekday">
                        {{ day }}
                    </div>

                    <!-- Kalendertage -->
                    <div v-for="day in getMonthWorkshops()"
                         :key="day.date.toISOString()"
                         class="day"
                         :class="{
                             'has-events': day.workshops.length > 0 || day.exchangeDays?.length > 0,
                             'has-exchange-day': day.exchangeDays?.length > 0,
                             'other-month': day.date.getMonth() !== currentDate.getMonth()
                         }"
                         @click.stop="switchToDay(day.date)">
                        <div class="day-number">{{ day.date.getDate() }}</div>

                        <!-- Exchange Days für den Tag -->
                        <div v-for="exchangeDay in day.exchangeDays"
                             :key="exchangeDay.id"
                             class="event exchange-day">
                            <span class="event-title">{{ exchangeDay.title }}</span>
                        </div>

                        <!-- Workshops für den Tag -->
                        <div class="events">
                            <div v-for="workshop in day.workshops"
                                 :key="workshop.id"
                                 class="event workshop"
                                 @click.stop="goToWorkshopDetails(workshop.id)">
                                <span class="event-time">{{ workshop.startTime }}</span>
                                <span class="event-title">{{ workshop.title }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Tagesansicht -->
            <div v-else class="day-view">
                <div class="calendar-header">
                    <Button class="nav-btn" @click="previousDay">&lt;</Button>
                    <h2>{{ currentDate.toLocaleDateString('de-DE', { weekday: 'long', day: 'numeric', month: 'long' }) }}</h2>
                    <Button class="nav-btn" @click="nextDay">&gt;</Button>
                </div>

                <!-- Exchange Days Banner -->
                <div v-if="currentDayExchangeDays.length > 0" class="exchange-days-container">
                    <!-- Erste zwei Exchange Days werden immer expandiert angezeigt -->
                    <div v-for="(exchangeDay, index) in currentDayExchangeDays.slice(0, 2)"
                         :key="exchangeDay.id"
                         class="exchange-day-banner">
                        <div class="exchange-day-content">
                            <!-- Exchange Day Header -->
                            <div class="exchange-day-header">
                                <div class="exchange-day-title">
                                    <h3>Exchangeday: {{ exchangeDay.title }}</h3>
                                    <div class="exchange-day-duration">
                                        {{ new Date(exchangeDay.startTime).toLocaleDateString('de-DE') }} -
                                        {{ new Date(exchangeDay.endTime).toLocaleDateString('de-DE') }}
                                    </div>
                                </div>
                                <div class="button-group">
                                    <Button class="action-btn" @click.stop="goToEditExchangeDay(exchangeDay.id)">
                                        Bearbeiten
                                    </Button>
                                    <Button class="action-btn delete" @click.stop="deleteExchangeDay(exchangeDay.id)">
                                        Löschen
                                    </Button>
                                </div>
                            </div>
                            <div class="content-wrapper">
                                <p>{{ exchangeDay.description }}</p>
                            </div>
                        </div>
                    </div>

                    <!-- Zusätzliche Exchange Days sind kollabierbar -->
                    <div v-if="currentDayExchangeDays.length > 2" class="collapsible-exchange-days">
                        <div v-for="exchangeDay in currentDayExchangeDays.slice(2)"
                             :key="exchangeDay.id"
                             class="exchange-day-banner collapsed"
                             :class="{ 'expanded': expandedExchangeDays.includes(exchangeDay.id) }">
                            <div class="exchange-day-content">
                                <div class="exchange-day-header"
                                     @click="toggleExchangeDay(exchangeDay.id)">
                                    <div class="exchange-day-title">
                                        <h3>
                                            <span class="expand-icon">
                                                {{ expandedExchangeDays.includes(exchangeDay.id) ? '▼' : '▶' }}
                                            </span>
                                            Exchangeday: {{ exchangeDay.title }}
                                        </h3>
                                    </div>
                                </div>
                                <div v-if="expandedExchangeDays.includes(exchangeDay.id)"
                                     class="collapsible-content">
                                    <div class="exchange-day-duration">
                                        {{ new Date(exchangeDay.startTime).toLocaleDateString('de-DE') }} -
                                        {{ new Date(exchangeDay.endTime).toLocaleDateString('de-DE') }}
                                    </div>
                                    <div class="content-wrapper">
                                        <p>{{ exchangeDay.description }}</p>
                                    </div>
                                    <div class="event-actions">
                                        <Button class="action-btn delete" @click.stop="deleteExchangeDay(exchangeDay.id)">
                                            Löschen
                                        </Button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- QR Code Modal -->
                <div v-if="showQRModal" class="modal">
                    <div class="modal-content qr-modal">
                        <div class="modal-header">
                            <h3>QR Code für Feedback</h3>
                            <button class="close-button" @click="showQRModal = false">&times;</button>
                        </div>
                        <div class="qr-container">
                            <div id="qrcode"></div>
                            <p class="qr-info">Scannen Sie den QR-Code um Feedback zu geben</p>
                        </div>
                    </div>
                </div>

                <!-- Tagesansicht Grid -->
                <div class="day-view-container">
                    <div class="day-schedule" :style="{
                        width: maxParallelWorkshops > 2 ? `${maxParallelWorkshops * 400}px` : '100%',
                        minWidth: '100%'
                    }">
                        <!-- Zeitachse -->
                        <div class="time-column">
                            <div v-for="hour in 12" :key="hour" class="time-slot">
                                {{ (hour + 7).toString().padStart(2, '0') }}:00
                            </div>
                        </div>

                        <!-- Workshop Grid -->
                        <div class="schedule-grid">
                            <div v-for="hour in 12" :key="hour" class="time-line"></div>

                            <!-- Workshop Karten -->
                            <template v-for="(group, groupIndex) in getOverlappingGroups(filteredDayWorkshops)" :key="groupIndex">
                                <div v-for="(workshop, workshopIndex) in group"
                                     :key="workshop.id"
                                     class="workshop-card"
                                     :style="{
                                         ...calculateEventStyle(workshop.startTime, workshop.endTime),
                                         left: `${(workshopIndex / group.length) * 100}%`,
                                         width: `${100 / group.length}%`,
                                         minWidth: '350px'
                                     }">
                                    <!-- Basis Workshop-Ansicht -->
                                    <div class="workshop-content-basic">
                                        <div class="workshop-header">
                                            <h3 class="workshop-title">{{ workshop.title }}</h3>
                                            <span class="workshop-time">{{ workshop.startTime }} - {{ workshop.endTime }}</span>
                                        </div>
                                        <div class="workshop-basic-actions">
                                            <Button class="action-btn" @click.stop="goToWorkshopDetails(workshop.id)">
                                                Details
                                            </Button>
                                        </div>
                                    </div>

                                    <!-- Erweiterte Workshop-Ansicht (Hover) -->
                                    <div class="workshop-content-expanded">
                                        <div class="workshop-header">
                                            <h3 class="workshop-title">{{ workshop.title }}</h3>
                                            <span class="workshop-time">{{ workshop.startTime }} - {{ workshop.endTime }}</span>
                                        </div>

                                        <div class="workshop-details">
                                            <p class="workshop-description">{{ workshop.description }}</p>
                                            <p><strong>Ort:</strong> {{ workshop.location }}</p>
                                            <p><strong>Max. Teilnehmer:</strong> {{ workshop.maxParticipants }}</p>
                                            <p v-if="workshop.trainer">
                                                <strong>Trainer:</strong> {{ workshop.trainer.firstName }} {{ workshop.trainer.lastName }}
                                            </p>
                                            <p v-if="workshop.exchangeDay">
                                                <strong>Exchange Day:</strong> {{ workshop.exchangeDay.title }}
                                            </p>
                                            <p><strong>Aktuelle Teilnehmer:</strong> {{ workshop.currentParticipants }}</p>
                                        </div>

                                        <div class="workshop-actions">
                                            <div class="button-group">
                                                <Button class="action-btn" @click.stop="goToWorkshopDetails(workshop.id)">
                                                    Details/Bearbeiten
                                                </Button>
                                                <Button class="action-btn delete" @click.stop="deleteWorkshop(workshop.id)">
                                                    Löschen
                                                </Button>
                                                <Button class="qr-btn"
                                                        @click.stop="openQRModal(workshop.id)">
                                                    QR Code
                                                </Button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </template>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* --- Basis Layout Styles --- */
.page-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    padding: 2rem;
}

.calendar {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
    background: white;
    border-radius: 16px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
    position: relative;
}

/* --- UI-State Styles --- */
.loading-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.8);
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 1.2rem;
    color: #2c3e50;
    z-index: 1000;
}

.error-message {
    background-color: #fee2e2;
    color: #dc2626;
    padding: 1rem;
    border-radius: 8px;
    margin-bottom: 1rem;
    text-align: center;
}

/* --- Control Styles --- */
.view-controls {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.button-group {
    display: flex;
    gap: 1rem;
}

:deep(.create-btn) {
    padding: 0.75rem 1.5rem;
    background-color: #27ae60;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

:deep(.create-btn:hover) {
    background-color: #219a52;
}

:deep(.toggle-view-btn) {
    padding: 0.75rem 1.5rem;
    background-color: #3498db;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

:deep(.toggle-view-btn:hover) {
    background-color: #2980b9;
}

/* --- Calendar Header Styles --- */
.calendar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.calendar-header h2 {
    font-size: 1.8rem;
    font-weight: 700;
    color: #2c3e50;
    margin: 0;
}

:deep(.nav-btn) {
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

:deep(.nav-btn:hover) {
    background-color: #2980b9;
}

/* --- Month View Grid Styles --- */
.calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 1px;
    background-color: #e9ecef;
    border: 1px solid #e9ecef;
    border-radius: 8px;
    overflow: hidden;
}

.weekday {
    background-color: #f8f9fa;
    padding: 10px;
    text-align: center;
    font-weight: 500;
    color: #2c3e50;
}

.day {
    background-color: white;
    min-height: 100px;
    padding: 10px;
    position: relative;
    cursor: pointer;
    transition: background-color 0.2s;
    display: flex;
    flex-direction: column;
}

.day:hover {
    background-color: #f8f9fa;
}

.day-number {
    position: absolute;
    top: 5px;
    right: 5px;
    color: #2c3e50;
    font-weight: 500;
    z-index: 2;
}

.has-events {
    background-color: #f8f9fa;
}

.has-exchange-day {
    background-color: #F3E5F5;
}

/* --- Event Card Styles --- */
.event {
    font-size: 0.8rem;
    padding: 4px 8px;
    margin-bottom: 4px;
    border-radius: 4px;
    cursor: pointer;
}

.event.workshop {
    background-color: #e3f2fd;
    color: #1976d2;
    border-left: 3px solid #3498db;
    flex: 1;
    min-height: 0;
    overflow: hidden;
}

.event-time {
    font-weight: 500;
    margin-right: 8px;
}

.event-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    margin-top: 1rem;
}

/* --- Exchange Day Styles --- */
.exchange-day {
    margin: 0 0 4px 0;
    padding: 4px 8px;
    background-color: #E1BEE7;
    color: #4a148c;
    border-left: 3px solid #9C27B0;
    border-radius: 4px;
    overflow: hidden;
}

.exchange-days-container {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    margin-bottom: 2rem;
}

.exchange-day-banner {
    background-color: #E1BEE7;
    margin: 0 0 2rem 0;
    padding: 1.5rem 2rem;
    border-radius: 12px;
    border-left: 4px solid #9C27B0;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.exchange-day-content {
    color: #4a148c;
}

.exchange-day-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
}

.exchange-day-title h3 {
    font-size: 1.4rem;
    margin: 0;
    font-weight: 600;
    color: #4a148c;
}

/* --- Day View Styles --- */
.day-view-container {
    max-width: none;
    overflow-x: auto;
    margin: 0 -2rem;
    padding: 0 2rem;
}

.day-schedule {
    position: relative;
    display: flex;
    background: white;
    border-radius: 8px;
    min-height: 720px;
    margin-top: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.time-column {
    width: 60px;
    flex-shrink: 0;
    border-right: 1px solid #e9ecef;
    background: #f8f9fa;
    z-index: 1;
}

.time-slot {
    height: 60px;
    padding: 8px;
    border-bottom: 1px solid #e9ecef;
    font-size: 0.8rem;
    color: #666;
}

.schedule-grid {
    flex-grow: 1;
    position: relative;
    padding: 0 10px;
    background: linear-gradient(#e9ecef 1px, transparent 1px);
    background-size: 100% 60px;
    min-width: calc(100% - 60px);
}

/* --- Workshop Card Styles --- */
.workshop-card {
    position: absolute;
    padding: 10px;
    transition: all 0.3s ease;
    border-radius: 8px;
    overflow: visible;
}

.workshop-content-basic {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: #e3f2fd;
    border-radius: 8px;
    border-left: 4px solid #3498db;
    padding: 10px;
    opacity: 1;
    visibility: visible;
    transition: opacity 0.2s ease;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    z-index: 1;
}

.workshop-content-expanded {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    background: #e3f2fd;
    border-radius: 8px;
    border-left: 4px solid #3498db;
    padding: 10px;
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    z-index: 2;
    min-height: max-content;
    transform-origin: top left;
    transform: scale(0.98);
}

/* --- Workshop Card Hover Effects --- */
.workshop-card:hover {
    z-index: 100;
}

.workshop-card:hover .workshop-content-basic {
    opacity: 0;
    visibility: hidden;
}

.workshop-card:hover .workshop-content-expanded {
    opacity: 1;
    visibility: visible;
    transform: scale(1);
}

/* --- Modal Styles --- */
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

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
}

.modal-header h3 {
    margin: 0;
    font-size: 1.5rem;
    color: #2c3e50;
}

.modal-content {
    background: white;
    padding: 2rem;
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.qr-modal {
    max-width: 400px;
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

/* --- Responsive Design --- */
@media (max-width: 768px) {
    .workshop-card {
        min-width: 280px;
    }
}

/* --- Utility Classes --- */
.empty-state {
    font-size: 1rem;
    font-weight: 600;
    margin: 0;
    color: #e74c3c;
}

/* --- QR Code Styles --- */
.qr-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 1rem;
}

.qr-info {
    margin-top: 1rem;
    text-align: center;
    color: #666;
    font-size: 0.9rem;
}

:deep(.qr-btn) {
    padding: 0.5rem 1rem;
    background-color: #9b59b6;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

:deep(.qr-btn:hover) {
    background-color: #8e44ad;
}

/* --- Scrollbar Styles --- */
.day-schedule::-webkit-scrollbar,
.day-view-container::-webkit-scrollbar {
    height: 8px;
}

.day-schedule::-webkit-scrollbar-track,
.day-view-container::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 4px;
}

.day-schedule::-webkit-scrollbar-thumb,
.day-view-container::-webkit-scrollbar-thumb {
    background: #888;
    border-radius: 4px;
}

.day-schedule::-webkit-scrollbar-thumb:hover,
.day-view-container::-webkit-scrollbar-thumb:hover {
    background: #555;
}

/* --- Exchange Day Content Styles --- */
.content-wrapper {
    overflow: auto;
    flex: 1;
    min-height: 0;
}

.exchange-day-content p {
    color: #6a1b9a;
    margin: 0.5rem 0 0 0;
    line-height: 1.5;
}

/* --- Collapsible Exchange Day Styles --- */
.collapsible-exchange-days {
    margin-top: 1rem;
}

.exchange-day-banner.collapsed {
    padding: 1rem 2rem;
    cursor: pointer;
}

.exchange-day-banner.collapsed:not(.expanded) {
    background-color: #F3E5F5;
}

.exchange-day-banner.collapsed:hover {
    background-color: #E1BEE7;
}

.expand-icon {
    display: inline-block;
    margin-right: 0.5rem;
    font-size: 0.8rem;
    transition: transform 0.2s ease;
}

.collapsible-content {
    margin-top: 1rem;
    animation: slideDown 0.3s ease-out;
}

.exchange-day-duration {
    font-size: 0.9rem;
    color: #6a1b9a;
    margin-top: 0.5rem;
}

/* --- Workshop Details Styles --- */
.workshop-header {
    margin-bottom: 8px;
}

.workshop-title {
    font-size: 1rem;
    font-weight: 600;
    margin: 0;
    color: #1976d2;
}

.workshop-time {
    font-size: 0.9rem;
    color: #1976d2;
    margin-top: 4px;
    display: block;
}

.workshop-details {
    font-size: 0.9rem;
    color: #1976d2;
    margin-top: 4px;
    display: block;
}

.workshop-description {
    margin-bottom: 8px;
}

.workshop-actions {
    margin-top: 10px;
    padding-top: 8px;
    border-top: 1px solid rgba(0,0,0,0.1);
}

.workshop-basic-actions {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid rgba(25, 118, 210, 0.2);
}

/* --- Animation --- */
@keyframes slideDown {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

:deep(.action-btn) {
    padding: 0.5rem 1rem;
    background-color: #3498db;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.9rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

:deep(.action-btn:hover) {
    background-color: #2980b9;
}

:deep(.action-btn.delete) {
    background-color: #e74c3c;
}

:deep(.action-btn.delete:hover) {
    background-color: #c0392b;
}
</style>


