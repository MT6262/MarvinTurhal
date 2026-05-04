<!--CreateWorkshopView.vue-->
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from "agnostic-vue"
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import axios from 'axios'
import { Toast, showToast } from "@/ts/toasts"
import config from '@/config';

// --- Interfaces ---
/**
 * Definiert ein Mitarbeiter-Objekt (Trainer)
 */
interface Employee {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    admin: boolean;
}

/**
 * Definiert ein Exchange Day-Objekt
 */
interface ExchangeDay {
    id: number;
    title: string;
    startTime: string;
    endTime: string;
}

// --- State Management ---
const router = useRouter()
const isLoading = ref(false)
const error = ref<string | null>(null)

// Daten-Zustand
const trainers = ref<Employee[]>([])
const exchangeDays = ref<ExchangeDay[]>([])
const selectedTrainerId = ref<number | null>(null)
const selectedExchangeDayId = ref<number | null>(null)

// Workshop-Formulardaten
const workshop = ref({
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    maxParticipants: 10,
    location: '',
    trainer: null as Employee | null,
    exchangeDay: null as ExchangeDay | null
})

// --- API Konfiguration ---
const axiosInstance = axios.create({
    baseURL: config.API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

// --- API Funktionen ---
/**
 * Lädt alle verfügbaren Trainer vom Server
 */
async function fetchTrainers() {
    try {
        const response = await axiosInstance.get('/trainers')
        trainers.value = response.data
    } catch (err) {
        showToast(new Toast(
            "Fehler",
            "Trainer konnten nicht geladen werden",
            "error",
            faExclamation
        ))
    }
}

/**
 * Lädt alle Exchange Days vom Server
 */
async function fetchExchangeDays() {
    try {
        const response = await axiosInstance.get('/exchangedays')
        exchangeDays.value = response.data
    } catch (err) {
        showToast(new Toast(
            "Fehler",
            "Exchange Days konnten nicht geladen werden",
            "error",
            faExclamation
        ))
    }
}

// --- Hilfsfunktionen ---
/**
 * Formatiert ein Datum in einen lokalen ISO-String
 */
function formatToLocalISOString(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// --- Formular Funktionen ---
/**
 * Erstellt einen neuen Workshop
 * Validiert die Eingaben und sendet die Daten an die API
 */
async function createWorkshop() {
    if (!validateForm()) {
        return
    }

    isLoading.value = true
    error.value = null

    try {
        const startDate = new Date(workshop.value.startTime);
        const endDate = new Date(workshop.value.endTime);

        await axiosInstance.post('/workshops', {
            ...workshop.value,
            startTime: formatToLocalISOString(startDate),
            endTime: formatToLocalISOString(endDate),
            trainer: selectedTrainerId.value ? { id: selectedTrainerId.value } : null,
            exchangeDay: selectedExchangeDayId.value ? { id: selectedExchangeDayId.value } : null
        })

        showToast(new Toast(
            "Erfolgreich",
            "Der Workshop wurde erfolgreich erstellt",
            "success",
            faCheck
        ))
        router.push('/admin')
    } catch (err: any) {
        console.error('Fehler beim Erstellen des Workshops:', err)
        showToast(new Toast(
            "Fehler",
            "Der Workshop konnte nicht erstellt werden",
            "error",
            faExclamation
        ))
    } finally {
        isLoading.value = false
    }
}

/**
 * Validiert die Formulareingaben
 * Prüft Pflichtfelder und Zeitbeschränkungen
 */
function validateForm(): boolean {
    if (!workshop.value.title.trim()) {
        showToast(new Toast("Fehler", "Bitte geben Sie einen Titel ein", "error", faExclamation));
        return false;
    }
    if (!selectedTrainerId.value) {
        showToast(new Toast("Fehler", "Bitte wählen Sie einen Trainer aus", "error", faExclamation));
        return false;
    }
    if (!workshop.value.startTime) {
        showToast(new Toast("Fehler", "Bitte wählen Sie eine Startzeit", "error", faExclamation));
        return false;
    }
    if (!workshop.value.endTime) {
        showToast(new Toast("Fehler", "Bitte wählen Sie eine Endzeit", "error", faExclamation));
        return false;
    }

    const startTime = new Date(workshop.value.startTime);
    const endTime = new Date(workshop.value.endTime);
    const startHour = startTime.getHours();
    const endHour = endTime.getHours();

    if (startHour < 8 || startHour >= 19 || endHour < 8 || endHour > 19) {
        showToast(new Toast("Fehler", "Workshops müssen zwischen 8:00 und 19:00 Uhr stattfinden", "error", faExclamation));
        return false;
    }

    return true;
}

/**
 * Berechnet die Zeitbeschränkungen (8 bis 19 Uhr)
 */
function getWorkingHourConstraints(date: string): { min: string, max: string } {
    const selectedDate = new Date(date);
    const minTime = new Date(selectedDate);
    const maxTime = new Date(selectedDate);

    minTime.setHours(8, 0, 0);
    maxTime.setHours(19, 0, 0);

    return {
        min: formatToLocalISOString(minTime),
        max: formatToLocalISOString(maxTime)
    };
}

// --- Watchers ---
// Setzt automatisch die Endzeit eine Stunde nach der Startzeit
watch(() => workshop.value.startTime, (newStartTime) => {
    if (newStartTime) {
        const startDate = new Date(newStartTime);
        const endDate = new Date(startDate);
        endDate.setHours(startDate.getHours() + 1);
        workshop.value.endTime = formatToLocalISOString(endDate);
    }
})

// Aktualisiert Workshop-Zeiten basierend auf Exchange Day-Auswahl
watch(() => selectedExchangeDayId.value, async (newId) => {
    if (newId) {
        const selectedExchangeDay = exchangeDays.value.find(ed => ed.id === newId);
        if (selectedExchangeDay) {
            workshop.value.startTime = formatToLocalISOString(new Date(selectedExchangeDay.startTime));
            workshop.value.endTime = workshop.value.startTime;
        }
    }
});

// --- Navigation ---
function goBack() {
    router.push('/admin')
}

// --- Lifecycle Hooks ---
onMounted(async () => {
    await Promise.all([fetchTrainers(), fetchExchangeDays()])
})
</script>

<template>
    <div class="page-container">
        <div class="form-container">
            <!-- Header mit Titel und Zurück-Button -->
            <div class="form-header">
                <h1>Workshop erstellen</h1>
                <Button class="back-btn" @click="goBack">Zurück</Button>
            </div>

            <!-- Hauptformular -->
            <form @submit.prevent="createWorkshop" class="workshop-form">
                <!-- Basis-Informationen -->
                <div class="form-group">
                    <label for="title">Titel<span class="required">*</span></label>
                    <input
                        id="title"
                        v-model="workshop.title"
                        type="text"
                        required
                        placeholder="Workshop-Titel"
                    >
                </div>

                <div class="form-group">
                    <label for="description">Beschreibung</label>
                    <textarea
                        id="description"
                        v-model="workshop.description"
                        placeholder="Workshop-Beschreibung"
                        rows="4"
                    ></textarea>
                </div>

                <!-- Trainer und Exchange Day Auswahl -->
                <div class="form-group">
                    <label for="trainer">Trainer<span class="required">*</span></label>
                    <select
                        id="trainer"
                        v-model="selectedTrainerId"
                        class="form-control"
                        required
                    >
                        <option :value="null">Kein Trainer ausgewählt</option>
                        <option
                            v-for="trainer in trainers"
                            :key="trainer.id"
                            :value="trainer.id"
                        >
                            {{ trainer.firstName }} {{ trainer.lastName }}
                        </option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="exchangeDay">Exchange Day</label>
                    <select
                        id="exchangeDay"
                        v-model="selectedExchangeDayId"
                        class="form-control"
                    >
                        <option :value="null">Kein Exchange Day ausgewählt</option>
                        <option
                            v-for="exchangeDay in exchangeDays"
                            :key="exchangeDay.id"
                            :value="exchangeDay.id"
                        >
                            {{ exchangeDay.title }}
                        </option>
                    </select>
                </div>

                <!-- Zeitauswahl -->
                <div class="form-row">
                    <div class="form-group">
                        <label for="startTime">Startzeit<span class="required">*</span></label>
                        <input
                            id="startTime"
                            v-model="workshop.startTime"
                            type="datetime-local"
                            required
                            class="calendar-input"
                            :min="workshop.startTime ? getWorkingHourConstraints(workshop.startTime).min : undefined"
                            :max="workshop.startTime ? getWorkingHourConstraints(workshop.startTime).max : undefined"
                        >
                    </div>

                    <div class="form-group">
                        <label for="endTime">Endzeit<span class="required">*</span></label>
                        <input
                            id="endTime"
                            v-model="workshop.endTime"
                            type="datetime-local"
                            required
                            class="calendar-input"
                            :min="workshop.startTime"
                            :max="workshop.startTime ? getWorkingHourConstraints(workshop.startTime).max : undefined"
                        >
                    </div>
                </div>

                <!-- Ort und Teilnehmer -->
                <div class="form-row">
                    <div class="form-group">
                        <label for="location">Ort<span class="required">*</span></label>
                        <input
                            id="location"
                            v-model="workshop.location"
                            type="text"
                            required
                            placeholder="Ort des Workshops"
                        >
                    </div>

                    <div class="form-group">
                        <label for="maxParticipants">Maximale Teilnehmerzahl<span class="required">*</span></label>
                        <input
                            id="maxParticipants"
                            v-model="workshop.maxParticipants"
                            type="number"
                            required
                            min="1"
                            class="number-input"
                        >
                    </div>
                </div>

                <!-- Formular-Aktionen -->
                <div class="form-actions">
                    <Button
                        type="submit"
                        class="submit-btn"
                        :disabled="isLoading"
                    >
                        {{ isLoading ? 'Wird erstellt...' : 'Workshop erstellen' }}
                    </Button>
                </div>
            </form>
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

.form-container {
    max-width: 800px;
    margin: 0 auto;
    padding: 2rem;
    background: white;
    border-radius: 16px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

/* --- Header Styles --- */
.form-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
}

.form-header h1 {
    font-size: 2.5rem;
    font-weight: bold;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    color: #2c3e50;
    margin: 0;
}

/* --- Form Layout Styles --- */
.workshop-form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
}

/* --- Input & Label Styles --- */
label {
    font-weight: 500;
    color: #2c3e50;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

input, textarea {
    padding: 0.75rem;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 1rem;
    transition: all 0.3s ease;
    background-color: #f8f9fa;
    color: #2c3e50;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

input:focus, textarea:focus {
    outline: none;
    border-color: #409eff;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

textarea {
    resize: none;
    height: 120px;
    overflow-y: auto;
    min-height: 120px;
    max-height: 300px;
}

/* --- Spezielle Input Styles --- */
.calendar-input {
    padding-right: 2rem;
    cursor: pointer;
}

.calendar-input::-webkit-calendar-picker-indicator {
    cursor: pointer;
    padding: 0.2rem;
    filter: invert(50%) sepia(8%) saturate(709%) hue-rotate(182deg) brightness(91%) contrast(83%);
    opacity: 0.7;
    transition: opacity 0.3s ease;
}

.calendar-input::-webkit-calendar-picker-indicator:hover {
    opacity: 1;
}

.number-input {
    padding-right: 1.5rem;
}

.number-input::-webkit-inner-spin-button,
.number-input::-webkit-outer-spin-button {
    opacity: 1;
    cursor: pointer;
    padding: 0.5rem;
}

/* --- Select Styles --- */
select.form-control {
    padding: 0.75rem;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 1rem;
    transition: all 0.3s ease;
    background-color: #f8f9fa;
    color: #2c3e50;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    cursor: pointer;
}

select.form-control:focus {
    outline: none;
    border-color: #409eff;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* --- Button Styles --- */
:deep(.back-btn) {
    padding: 0.5rem 1rem;
    background-color: #e74c3c;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    transition: background-color 0.3s ease;
}

:deep(.back-btn:hover) {
    background-color: #c0392b;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 1rem;
}

:deep(.submit-btn) {
    padding: 0.75rem 1.5rem;
    background-color: #27ae60;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

:deep(.submit-btn:hover) {
    background-color: #219a52;
}

:deep(.submit-btn:disabled) {
    background-color: #a0aec0;
    cursor: not-allowed;
}

/* --- Utility Styles --- */
.required {
    color: #e74c3c;
    margin-left: 4px;
}

/* --- Responsive Styles --- */
@media (max-width: 768px) {
    .page-container {
        padding: 1rem;
    }

    .form-container {
        padding: 1rem;
    }

    .form-row {
        grid-template-columns: 1fr;
    }
}
</style>
