<!--EmployeeCalendarView-->
<script setup lang="ts">
import { Button } from "agnostic-vue"
import { useRouter } from 'vue-router'
import axios from 'axios'
import {Toast, showToast, activeToasts} from "@/ts/toasts"
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import config from '@/config';
import {ref, computed, onMounted, onBeforeUnmount, nextTick} from 'vue'

const router = useRouter()

interface Workshop {
    id: number;
    title: string;
    description: string;
    startTime: string;
    endTime: string;
    currentParticipants: number;
    maxParticipants: number;
    location: string;
    date?: Date;
    type?: string;
    isRegistered?: boolean;
}

interface ExchangeDay {
    id: number;
    title: string;
    description: string;
    startTime: string;
    endTime: string;
    date?: Date;
}


const workshops = ref<Workshop[]>([]);
const exchangeDays = ref<ExchangeDay[]>([]);
const currentDate = ref(new Date());
const viewType = ref<'month' | 'day'>('month');
const weekDays = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So'];
const isLoading = ref(false);
const error = ref<string | null>(null);
const showTrainerModal = ref(false);
const selectedTrainer = ref<Employee | null>(null);
const expandedExchangeDays = ref<number[]>([]);
const selectedWorkshopId = ref<number | null>(null);
const showQRModal = ref(false);
const trainerStats = ref({
    averageRating: 0,
    totalRatings: 0
});

const maxParallelWorkshops = computed(() => {
    const groups = getOverlappingGroups(filteredDayWorkshops.value);
    return Math.max(...groups.map(group => group.length), 1);
});

const axiosInstance = axios.create({
    baseURL: config.API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
});


async function fetchWorkshops() {
    isLoading.value = true;
    error.value = null;
    try {
        const employeeId = localStorage.getItem('employeeId');
        if (!employeeId) {
            router.push('/');
            return;
        }

        // Hole alle Workshops
        const workshopsResponse = await axiosInstance.get('/workshops');
        // Hole alle Registrierungen
        const registrationsResponse = await axiosInstance.get('/registrations');

        if (workshopsResponse.data) {
            workshops.value = workshopsResponse.data.map((workshop: any) => {
                const startDateTime = new Date(workshop.startTime);
                const endDateTime = new Date(workshop.endTime);

                // Prüfe ob der aktuelle Benutzer für diesen Workshop registriert ist
                const isRegistered = registrationsResponse.data.some(
                    (r: any) => r.workshop.id === workshop.id &&
                        r.employee.id === parseInt(employeeId)
                );

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
                    isRegistered: isRegistered
                };
            });
            showNextWorkshopToast();
        }
    } catch (err: any) {
        console.error('Error fetching workshops:', err);
        workshops.value = [];
        showToast(new Toast("Fehler", "Workshops konnten nicht geladen werden", "error", faExclamation));
    } finally {
        isLoading.value = false;
    }
}

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
        console.error('Error fetching exchange days:', err);
        showToast(new Toast("Fehler", "Exchange Days konnten nicht geladen werden", "error", faExclamation));
    }
}

async function registerForWorkshop(workshopId: number) {
    try {
        const employeeId = localStorage.getItem('employeeId');

        if (!employeeId) {
            showToast(new Toast(
                "Fehler",
                "Bitte melden Sie sich erneut an",
                "error",
                faExclamation
            ));
            router.push('/');
            return;
        }
        const response = await axiosInstance.post('/registrations', {
            employeeId: parseInt(employeeId),
            workshopId: workshopId,
            exchangeDayId: 0
        });

        if (response.status === 200 || response.status === 201) {
            await fetchWorkshops();
            showToast(new Toast("Erfolg", "Für Workshop angemeldet", "success", faCheck));
        }
    } catch (err: any) {
        console.error('Error registering for workshop:', err);
        const errorMessage = err.response?.data?.message || "Anmeldung fehlgeschlagen";
        showToast(new Toast(
            "Fehler",
            errorMessage,
            "error",
            faExclamation
        ));
    }
}

async function unregisterFromWorkshop(workshopId: number) {
    try {
        const employeeId = localStorage.getItem('employeeId');

        if (!employeeId) {
            showToast(new Toast(
                "Fehler",
                "Bitte melden Sie sich erneut an",
                "error",
                faExclamation
            ));
            router.push('/');
            return;
        }

        // Finde zuerst die Registration ID
        const registrationsResponse = await axiosInstance.get('/registrations');
        const registration = registrationsResponse.data.find(
            (r: any) => r.employee.id === parseInt(employeeId) && r.workshop.id === workshopId
        );

        if (!registration) {
            throw new Error('Keine Anmeldung gefunden');
        }

        // Lösche die Registration
        await axiosInstance.delete(`/registrations/${registration.id}`);

        // Aktualisiere den Workshop im Array
        const workshopIndex = workshops.value.findIndex(w => w.id === workshopId);
        if (workshopIndex !== -1) {
            workshops.value[workshopIndex].currentParticipants--;
            workshops.value[workshopIndex].isRegistered = false;
        }

        await fetchWorkshops();
        showToast(new Toast("Erfolg", "Vom Workshop abgemeldet", "success", faCheck));
    } catch (err: any) {
        console.error('Error unregistering from workshop:', err);
        const errorMessage = err.response?.data?.message || "Abmeldung fehlgeschlagen";
        showToast(new Toast(
            "Fehler",
            errorMessage,
            "error",
            faExclamation
        ));
    }
}

// Funktion zum Öffnen des Trainer-Modals
async function openTrainerModal(trainer: Employee) {
    try {
        // API-Aufruf um die Trainer-Statistiken zu laden
        const response = await axiosInstance.get(`/feedback/trainer/${trainer.id}/stats`);
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

// QR-Code Generierung
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

function goToFeedback(workshopId: number) {
    router.push(`/feedback/${workshopId}`);
}

function getMonthWorkshops() {
    const year = currentDate.value.getFullYear();
    const month = currentDate.value.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const days = [];

    const firstDayOfWeek = firstDay.getDay() || 7;
    for (let i = 1; i < firstDayOfWeek; i++) {
        days.push({
            date: new Date(year, month, 1 - (firstDayOfWeek - i)),
            workshops: [],
            exchangeDays: []
        });
    }

    for (let d = 1; d <= lastDay.getDate(); d++) {
        const day = new Date(year, month, d);
        day.setHours(0, 0, 0, 0);

        // Workshop-Filterung bleibt unverändert
        const dayWorkshops = workshops.value.filter(workshop => {
            if (!workshop.date) return false;
            return (
                workshop.date.getDate() === day.getDate() &&
                workshop.date.getMonth() === day.getMonth() &&
                workshop.date.getFullYear() === day.getFullYear()
            );
        });

        // Logik für mehrtägige Exchange Days
        const dayExchangeDays = exchangeDays.value.filter(exchangeDay => {
            const startDate = new Date(exchangeDay.startTime);
            const endDate = new Date(exchangeDay.endTime);

            startDate.setHours(0, 0, 0, 0);
            endDate.setHours(23, 59, 59, 999);

            // PrüfT ob der aktuelle Tag zwischen Start- und Enddatum liegt (inklusive)
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

function switchToDay(date: Date) {
    currentDate.value = date;
    viewType.value = 'day';
}

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

// Funktion um den nächsten Workshop zu finden
function getNextWorkshop() {
    const now = new Date();
    const registeredWorkshops = workshops.value.filter(workshop =>
        workshop.isRegistered && workshop.date && workshop.date > now
    );

    return registeredWorkshops.sort((a, b) =>
        (a.date?.getTime() || 0) - (b.date?.getTime() || 0)
    )[0];
}

function showNextWorkshopToast() {
    const nextWorkshop = getNextWorkshop();
    if (nextWorkshop && nextWorkshop.date) {
        const formattedDate = nextWorkshop.date.toLocaleDateString('de-DE', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
        });

        // Füge die Uhrzeit hinzu
        const formattedTime = `${nextWorkshop.startTime} - ${nextWorkshop.endTime}`;

        showToast(new Toast(
            "Ihr nächster Workshop",
            `${nextWorkshop.title} am ${formattedDate} um ${formattedTime} in ${nextWorkshop.location}`,
            "info",
            faCheck,
            0,
            true,
            'next-workshop-toast'
        ));
    }
}

function toggleExchangeDay(id: number) {
    const index = expandedExchangeDays.value.indexOf(id);
    if (index === -1) {
        expandedExchangeDays.value.push(id);
    } else {
        expandedExchangeDays.value.splice(index, 1);
    }
}

function calculateEventStyle(startTime: string, endTime: string): { top: string, height: string, left: string, width: string } {
    const startHour = parseInt(startTime.split(':')[0]);
    const startMinute = parseInt(startTime.split(':')[1]);
    const endHour = parseInt(endTime.split(':')[0]);
    const endMinute = parseInt(endTime.split(':')[1]);

    // Begrenzen der Zeiten auf 8-19 Uhr
    const adjustedStartHour = Math.max(8, Math.min(19, startHour));
    const adjustedEndHour = Math.max(8, Math.min(19, endHour));

    const startFromEight = (adjustedStartHour - 8) * 60 + startMinute;
    const duration = ((adjustedEndHour - adjustedStartHour) * 60 + (endMinute - startMinute));

    const top = `${(startFromEight / 60) * 60}px`;
    const height = `${(duration / 60) * 60}px`;

    return { top, height, left: '0', width: '100%' };
}

// getOverlappingGroups Funktion für bessere Gruppierung
function getOverlappingGroups(workshops: Workshop[]): Workshop[][] {
    const groups: Workshop[][] = [];

    // Sortiere Workshops nach Startzeit
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


onMounted(async () => {
    await Promise.all([fetchWorkshops(), fetchExchangeDays()]);
});
onBeforeUnmount(() => {
    const existingToast = activeToasts.value.find(toast => toast.className === 'next-workshop-toast');
    if (existingToast) {
        existingToast.close();
    }
});
</script>

<template>
    <div class="page-container">
        <!-- Trainer Modal -->
        <div v-if="showTrainerModal" class="modal">
            <div class="modal-content trainer-modal">
                <div class="modal-header">
                    <button class="close-button" @click="showTrainerModal = false">&times;</button>
                </div>
                <div class="trainer-info">
                    <h4>{{ selectedTrainer?.firstName }} {{ selectedTrainer?.lastName }}</h4>
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
        <div class="calendar">
            <div v-if="isLoading" class="loading-overlay">
                Lädt...
            </div>

            <div v-if="error" class="error-message">
                {{ error }}
            </div>

            <div v-if="!isLoading && workshops.length === 0" class="empty-state">
                <h3>Keine Workshops vorhanden</h3>
                <p>Derzeit sind keine Workshops verfügbar.</p>
            </div>

            <div class="view-controls">
                <Button class="toggle-view-btn" @click="toggleView">
                    {{ viewType === 'month' ? 'Tagesansicht' : 'Monatsansicht' }}
                </Button>
                <div class="button-group">
                    <Button
                        class="feedback-overview-btn"
                        @click="router.push('/my-feedback')"
                    >
                        Mein Feedback
                    </Button>
                    <Button
                        class="dashboard-btn"
                        @click="router.push('/employee-dashboard')"
                    >
                        Dashboard
                    </Button>
                </div>
            </div>

            <div v-if="viewType === 'month'">
                <div class="calendar-header">
                    <Button class="nav-btn" @click="previousMonth">&lt;</Button>
                    <h2>{{ currentDate.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' }) }}</h2>
                    <Button class="nav-btn" @click="nextMonth">&gt;</Button>
                </div>

                <div class="calendar-grid">
                    <div v-for="day in weekDays" :key="day" class="weekday">
                        {{ day }}
                    </div>

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

                        <!-- Exchange Days -->
                        <div v-for="exchangeDay in day.exchangeDays"
                             :key="exchangeDay.id"
                             class="event exchange-day">
                            <span class="event-title">{{ exchangeDay.title }}</span>
                        </div>

                        <!-- Workshops -->
                        <div class="events">
                            <div v-for="workshop in day.workshops"
                                 :key="workshop.id"
                                 class="event workshop">
                                <span class="event-time">{{ workshop.startTime }}</span>
                                <span class="event-title">{{ workshop.title }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div v-else class="day-view">
                <div class="calendar-header">
                    <Button class="nav-btn" @click="previousDay">&lt;</Button>
                    <h2>{{ currentDate.toLocaleDateString('de-DE', { weekday: 'long', day: 'numeric', month: 'long' }) }}</h2>
                    <Button class="nav-btn" @click="nextDay">&gt;</Button>
                </div>

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

                <!-- Exchange Days Banner -->
                <div v-if="currentDayExchangeDays.length > 0" class="exchange-days-container">
                    <!-- Erste zwei Exchange Days werden immer expandiert angezeigt -->
                    <div v-for="(exchangeDay, index) in currentDayExchangeDays.slice(0, 2)"
                         :key="exchangeDay.id"
                         class="exchange-day-banner">
                        <div class="exchange-day-content">
                            <div class="exchange-day-header">
                                <div class="exchange-day-title">
                                    <h3>Exchangeday: {{ exchangeDay.title }}</h3>
                                    <div class="exchange-day-duration">
                                        {{ new Date(exchangeDay.startTime).toLocaleDateString('de-DE') }} -
                                        {{ new Date(exchangeDay.endTime).toLocaleDateString('de-DE') }}
                                    </div>
                                </div>
                            </div>
                            <div class="content-wrapper">
                                <p>{{ exchangeDay.description }}</p>
                            </div>
                        </div>
                    </div>

                    <!-- Additional Exchange Days are collapsible -->
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
                                            <span class="expand-icon">{{ expandedExchangeDays.includes(exchangeDay.id) ? '▼' : '▶' }}</span>
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
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Container für scrollbare Tagesansicht -->
                <div class="day-view-container">
                    <div class="day-schedule" :style="{
                        width: maxParallelWorkshops > 2 ? `${maxParallelWorkshops * 400}px` : '100%',
                        minWidth: '100%'
                    }">
                        <div class="time-column">
                            <div v-for="hour in 12" :key="hour" class="time-slot">
                                {{ (hour + 7).toString().padStart(2, '0') }}:00
                            </div>
                        </div>

                        <div class="schedule-grid">
                            <!-- Zeitlinien -->
                            <div v-for="hour in 12" :key="hour" class="time-line"></div>

                            <!-- Workshops -->
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
                                    <div class="workshop-content">
                                        <div class="workshop-header">
                                            <h3 class="workshop-title">{{ workshop.title }}</h3>
                                            <span class="workshop-time">{{ workshop.startTime }} - {{ workshop.endTime }}</span>
                                        </div>

                                        <div class="workshop-details">
                                            <p class="workshop-description">{{ workshop.description }}</p>
                                            <p><strong>Ort:</strong> {{ workshop.location }}</p>
                                            <p><strong>Teilnehmer:</strong> {{ workshop.currentParticipants }} / {{ workshop.maxParticipants }}</p>
                                            <p v-if="workshop.trainer" class="trainer-name">
                                                <strong>Trainer:</strong>
                                                <span class="clickable-trainer" @click.stop="openTrainerModal(workshop.trainer)">
                                                    {{ workshop.trainer.firstName }} {{ workshop.trainer.lastName }}
                                                </span>
                                            </p>
                                            <p v-if="workshop.exchangeDay"><strong>Exchange Day:</strong> {{ workshop.exchangeDay.title }}</p>
                                        </div>

                                        <div class="workshop-actions">
                                            <div class="button-group">
                                                <Button v-if="!workshop.isRegistered"
                                                        class="action-btn register"
                                                        @click.stop="registerForWorkshop(workshop.id)"
                                                        :disabled="workshop.currentParticipants >= workshop.maxParticipants">
                                                    Anmelden
                                                </Button>
                                                <template v-else>
                                                    <Button class="nav-btn unregister"
                                                            @click.stop="unregisterFromWorkshop(workshop.id)">
                                                        Abmelden
                                                    </Button>
                                                    <Button class="toggle-view-btn"
                                                            @click.stop="goToFeedback(workshop.id)">
                                                        Feedback
                                                    </Button>
                                                    <Button class="qr-btn"
                                                            @click.stop="openQRModal(workshop.id)">
                                                        QR Code
                                                    </Button>
                                                </template>
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

.exchange-day {
    margin: 0 0 4px 0;
    padding: 4px 8px;
    background-color: #E1BEE7;
    color: #4a148c;
    border-left: 3px solid #9C27B0;
    border-radius: 4px;
    overflow: hidden;
}

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

.day-schedule {
    position: relative;
    display: flex;
    background-color: #f8f9fa;
    border-radius: 8px;
    min-height: 720px;
    margin-top: 20px;
}

.time-slots {
    width: 80px;
    flex-shrink: 0;
    border-right: 1px solid #e9ecef;
    z-index: 1;
}

.time-slot {
    height: 60px;
    padding: 8px;
    border-bottom: 1px solid #e9ecef;
    background-color: white;
}

.time {
    font-size: 0.9rem;
    color: #2c3e50;
    font-weight: 500;
}

.events-container {
    flex-grow: 1;
    position: relative;
    padding: 0 10px;
    background: linear-gradient(#e9ecef 1px, transparent 1px);
    background-size: 100% 60px;
}

.day-event {
    position: absolute;
    left: 10px;
    right: 10px;
    display: flex;
    flex-direction: column;
    background-color: #e3f2fd;
    border-left: 4px solid #3498db;
    border-radius: 8px;
    padding: 8px;
    overflow: hidden;
    z-index: 2;
}

.day-event.workshop {
    background-color: #EBF5FB;
    border-left: 4px solid #2980b9;
    color: #2c3e50;
}

.day-event.parallel-event {
    width: calc(50% - 15px);
    box-sizing: border-box;
    margin-bottom: 10px;
}

.day-event.parallel-event:nth-child(odd) {
    left: 10px;
}

.day-event.parallel-event:nth-child(even) {
    left: calc(50% + 5px);
}
.event-actions .action-btn.register {
    background-color: #27ae60;
    color: white;
    padding: 0.5rem 1rem;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.9rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

.event-actions .action-btn.register:disabled {
    background-color: #a0aec0;
    cursor: not-allowed;
}

.event-actions .action-btn.register:hover:not(:disabled) {
    background-color: #219a52;
}

.no-workshops-message {
    text-align: center;
    color: #7f8c8d;
    padding: 2rem;
    font-size: 1.1rem;
}

.button-group {
    display: flex;
    gap: 1rem;
    margin-top: 1rem;
}

:deep(.nav-btn.unregister) {
    padding: 0.5rem 1rem;
    background-color: #e74c3c;  /* Rot, passend zum Design */
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

:deep(.nav-btn.unregister:hover) {
    background-color: #c0392b;
}

.event-actions :deep(.toggle-view-btn) {
    padding: 0.5rem 1rem;
    background-color: #27ae60;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

.event-actions :deep(.toggle-view-btn:hover) {
    background-color: #219a52;
}

.event-actions :deep(.action-btn.register) {
    padding: 0.5rem 1rem;
    background-color: #27ae60;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

.event-actions :deep(.action-btn.register:hover:not(:disabled)) {
    background-color: #219a52;
}

.event-actions :deep(.action-btn.register:disabled) {
    background-color: #a0aec0;
    cursor: not-allowed;
}

.action-btn {
    padding: 8px 16px;
    border-radius: 4px;
    font-weight: 500;
}

.action-btn.register {
    background-color: #27ae60;
    color: white;
}

.action-btn.unregister {
    background-color: #f44336;
    color: white;
}

.action-btn.feedback {
    background-color: #4CAF50;
    color: white;
}

.action-btn:disabled {
    background-color: #cccccc;
    cursor: not-allowed;
}

:deep(.feedback-overview-btn) {
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

.trainer-modal {
    max-width: 400px;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    font-size: 1rem;
    font-weight: 700;
    color: #2c3e50;
    text-align: left;
    line-height: 1.2;
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

.clickable-trainer {
    color: #3498db;
    cursor: pointer;
    text-decoration: underline;
}

.clickable-trainer:hover {
    color: #2980b9;
}

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
    border-radius: 8px;
    box-shadow: 0 10px 25px rgba(0,0,0,0.2);
}

.modal-footer {
    margin-top: 1.5rem;
    display: flex;
    justify-content: flex-end;
}

:deep(.dashboard-btn) {
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

:deep(.dashboard-btn:hover) {
    background-color: #42b983;
}

.day-schedule {
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
}

.time-line {
    position: absolute;
    left: 0;
    right: 0;
    height: 1px;
    background: #e9ecef;
    pointer-events: none;
}

.workshop-card {
    position: absolute;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    border-left: 4px solid #3498db;
    padding: 10px;
    overflow: hidden;
    transition: all 0.3s ease;
}

.workshop-card:hover {
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
    transform: translateY(-1px);
}

.workshop-content {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.workshop-header {
    margin-bottom: 8px;
}

.workshop-header h3 {
    margin: 0;
    font-size: 1rem;
    color: #1976d2;
}

.workshop-time {
    font-size: 0.8rem;
    color: #666;
}

.workshop-details {
    flex-grow: 1;
    overflow-y: auto;
    font-size: 0.9rem;
}

.workshop-description {
    margin-bottom: 8px;
}

.workshop-actions {
    margin-top: auto;
    display: flex;
    gap: 8px;
}

/* Responsive Design */
@media (max-width: 768px) {
    .workshop-card {
        left: 0 !important;
        width: calc(100% - 20px) !important;
    }
}

.day-schedule {
    display: flex;
    background: white;
    border-radius: 8px;
    min-height: 720px;
    margin-top: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    overflow-x: auto; /* Ermöglicht horizontales Scrollen */
}

.schedule-grid {
    flex-grow: 1;
    position: relative;
    padding: 0 10px;
    background: linear-gradient(#e9ecef 1px, transparent 1px);
    background-size: 100% 60px;
    min-width: calc(100% - 60px); /* Berücksichtigt die Breite der Zeitspalte */
}

.workshop-card {
    position: absolute;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    border-left: 4px solid #3498db;
    padding: 10px;
    overflow: hidden;
    transition: all 0.3s ease;
}

.calendar {
    max-width: none;
    margin: 0 auto;
    padding: 2rem;
}

.day-schedule::-webkit-scrollbar {
    height: 8px;
}

.day-schedule::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 4px;
}

.day-schedule::-webkit-scrollbar-thumb {
    background: #888;
    border-radius: 4px;
}

.day-schedule::-webkit-scrollbar-thumb:hover {
    background: #555;
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

/* für den Container der Tagesansicht */
.day-view-container {
    max-width: none;
    overflow-x: auto; /* Ermöglicht horizontales Scrollen */
    margin: 0 -2rem;
    padding: 0 2rem;
}

.day-schedule {
    display: flex;
    background: white;
    border-radius: 8px;
    min-height: 720px;
    margin-top: 20px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}


.day-view-container::-webkit-scrollbar {
    height: 8px;
}

.day-view-container::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 4px;
}

.day-view-container::-webkit-scrollbar-thumb {
    background: #888;
    border-radius: 4px;
}

.day-view-container::-webkit-scrollbar-thumb:hover {
    background: #555;
}

.workshop-card {
    position: absolute;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    border-left: 4px solid #3498db;
    padding: 10px;
    overflow: hidden;
    transition: all 0.3s ease;
}

/* Basis-Ansicht Styles */
.workshop-content-basic {
    opacity: 1;
    transition: opacity 0.2s ease;
}

.workshop-content-expanded {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: white;
    padding: 10px;
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s ease;
    display: flex;
    flex-direction: column;
    z-index: 10;
}

/* Hover-Effekt */
.workshop-card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    z-index: 100; /* Bringt die Karte nach vorne */
}

.workshop-card:hover .workshop-content-basic {
    opacity: 0;
}

.workshop-card:hover .workshop-content-expanded {
    opacity: 1;
    visibility: visible;
}

/* Basis-Ansicht */
.workshop-title {
    font-size: 1rem;
    font-weight: 600;
    margin: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.workshop-time {
    font-size: 0.8rem;
    color: #666;
    display: block;
    margin-top: 4px;
}


.workshop-header {
    margin-bottom: 8px;
}

.workshop-details {
    flex-grow: 1;
    overflow-y: auto;
    font-size: 0.9rem;
}

.workshop-description {
    margin-bottom: 8px;
}

.workshop-actions {
    margin-top: auto;
}


.workshop-card:hover .workshop-content-expanded {
    min-height: 200px;
    height: auto;
    max-height: 400px;
}

.workshop-card {
    position: absolute;
    background: #e3f2fd;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    border-left: 4px solid #3498db;
    padding: 10px;
    overflow: visible;
    transition: all 0.3s ease;
}

/* Basis-Ansicht Styles */
.workshop-content-basic {
    opacity: 1;
    transition: opacity 0.2s ease;
    color: #1976d2;
}

.workshop-content-expanded {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    background: #e3f2fd;
    padding: 10px;
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s ease;
    display: flex;
    flex-direction: column;
    z-index: 10;
    min-height: 200px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    border-radius: 8px;
    border-left: 4px solid #3498db;
}

/* Hover-Effekt */
.workshop-card:hover {
    z-index: 100;
}

.workshop-card:hover .workshop-content-basic {
    opacity: 0;
}

.workshop-card:hover .workshop-content-expanded {
    opacity: 1;
    visibility: visible;
    min-height: max-content;
    height: auto;
    /* Stellt sicher, dass die erweiterte Ansicht immer nach oben geht, wenn unten kein Platz ist */
    bottom: auto;
    top: 0;
    /* Wenn der Workshop nahe am unteren Rand ist, zeigt nach oben */
    &:has(+ .schedule-grid[style*="bottom: 0"]) {
        top: auto;
        bottom: 0;
    }
}

/* Basis-Ansicht */
.workshop-title {
    font-size: 1rem;
    font-weight: 600;
    margin: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: #1976d2;
}

.workshop-time {
    font-size: 0.8rem;
    color: #1976d2;
    display: block;
    margin-top: 4px;
}

/* Erweiterte Ansicht Styles */
.workshop-header {
    margin-bottom: 8px;
}

.workshop-details {
    flex-grow: 1;
    overflow-y: auto;
    font-size: 0.9rem;
    margin: 10px 0;
    max-height: 200px;
}

.workshop-description {
    margin-bottom: 8px;
}

.workshop-actions {
    margin-top: 10px;
    position: sticky;
    bottom: 0;
    background: #e3f2fd;
    padding-top: 5px;
    border-top: 1px solid rgba(0,0,0,0.1);
}


.button-group {
    display: flex;
    gap: 8px;
    margin-top: 8px;
    position: relative;
    z-index: 11;
}

.workshop-card {
    position: absolute;
    background: #e3f2fd;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    border-left: 4px solid #3498db;
    padding: 10px;
    overflow: visible;
    transition: all 0.3s ease;
}

.workshop-content-basic {
    opacity: 1;
    transition: opacity 0.2s ease;
    color: #1976d2;
    height: 100%;
    display: flex;
    flex-direction: column;
}

.workshop-basic-info {
    font-size: 0.9rem;
    margin-top: 8px;
}

.workshop-basic-info p {
    margin: 4px 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.workshop-basic-actions {
    margin-top: 8px;
}

.workshop-card {
    position: absolute;
    background: #e3f2fd;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    border-left: 4px solid #3498db;
    padding: 10px;
    overflow: hidden;
    transition: all 0.3s ease;
}

.workshop-content-basic {
    display: block;
    color: #1976d2;
    height: 100%;
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

.workshop-basic-info {
    margin-top: 8px;
}

.workshop-basic-info p {
    margin: 4px 0;
    font-size: 0.9rem;
    color: #1976d2;
}

.workshop-basic-actions {
    margin-top: 8px;
}


.workshop-card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    z-index: 100;
    background: #e3f2fd;
    height: auto !important;
    min-height: 200px;
}


.workshop-details {
    display: none;
    margin-top: 10px;
    font-size: 0.9rem;
    color: #1976d2;
}

.workshop-card:hover .workshop-details {
    display: block;
}

.workshop-actions {
    margin-top: 10px;
    border-top: 1px solid rgba(0, 0, 0, 0.1);
    padding-top: 8px;
}

.button-group {
    display: flex;
    gap: 8px;
}

.workshop-title {
    font-size: 1rem;
    font-weight: 600;
    margin: 0;
    color: #1976d2;
}

.close-trainerModal{
    padding: 0.8rem 1rem;
    background-color: #e74c3c;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

.empty-state{
    font-size: 1rem;
    font-weight: 600;
    margin: 0;
    color:#e74c3c;
}

.qr-modal {
    max-width: 400px;
}

.qr-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 1rem;
}

.qr-code {
    width: 250px;
    height: 250px;
    margin-bottom: 1rem;
}

.qr-info {
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
</style>
