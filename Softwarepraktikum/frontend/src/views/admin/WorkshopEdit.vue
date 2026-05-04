<!--WorkshopEdit-->
<script setup lang="ts">
import {ref, onMounted, watch} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button } from "agnostic-vue"
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import axios from 'axios'
import { Toast, showToast } from "@/ts/toasts"
import config from '@/config';

// --- Interfaces ---
/**
 * Definiert ein Mitarbeiter-Objekt
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

/**
 * Definiert ein Workshop-Objekt
 */
interface Workshop {
    id: number | null;
    title: string;
    description: string;
    startTime: string;
    endTime: string;
    maxParticipants: number;
    location: string;
    trainer: Employee | null;
    exchangeDay: ExchangeDay | null;
}

// --- State Management ---
const workshop = ref<Workshop>({
    id: null,
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    maxParticipants: 10,
    location: '',
    trainer: null,
    exchangeDay: null
});

const route = useRoute()
const router = useRouter()
const isLoading = ref(false)
const error = ref<string | null>(null)
const trainers = ref<Employee[]>([])
const exchangeDays = ref<ExchangeDay[]>([])
const selectedTrainerId = ref<number | null>(null)
const selectedExchangeDayId = ref<number | null>(null)

// --- API Konfiguration ---
const axiosInstance = axios.create({
    baseURL: config.API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

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

// --- API Funktionen ---
/**
 * Lädt den zu bearbeitenden Workshop
 */
async function fetchWorkshop() {
    const workshopId = route.params.id;
    isLoading.value = true;

    try {
        const response = await axiosInstance.get(`/workshops/${workshopId}`);
        const workshopData = response.data;

        const startTime = new Date(workshopData.startTime);
        const endTime = new Date(workshopData.endTime);

        workshop.value = {
            ...workshopData,
            startTime: formatToLocalISOString(startTime),
            endTime: formatToLocalISOString(endTime)
        };

        // Setze den Trainer wenn vorhanden
        if (workshopData.trainer) {
            selectedTrainerId.value = workshopData.trainer.id;
        }

        // Setze den Exchange Day wenn vorhanden
        if (workshopData.exchangeDay) {
            selectedExchangeDayId.value = workshopData.exchangeDay.id;
            workshop.value.exchangeDay = workshopData.exchangeDay;
        }
    } catch (err) {
        console.error('Error fetching workshop:', err);
        showToast(new Toast(
            "Fehler",
            "Workshop konnte nicht geladen werden",
            "error",
            faExclamation
        ));
    } finally {
        isLoading.value = false;
    }
}

/**
 * Lädt die verfügbaren Trainer
 */
async function fetchTrainers() {
    try {
        const response = await axiosInstance.get('/trainers')
        trainers.value = response.data
    } catch (err) {
        console.error('Error fetching trainers:', err)
        showToast(new Toast(
            "Fehler",
            "Trainer konnten nicht geladen werden",
            "error",
            faExclamation
        ))
    }
}

/**
 * Lädt die verfügbaren Exchange Days
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

/**
 * Aktualisiert den Workshop
 */
async function updateWorkshop() {
    if (!validateForm()) return;

    isLoading.value = true;
    error.value = null;

    try {
        const startDate = new Date(workshop.value.startTime);
        const endDate = new Date(workshop.value.endTime);

        await axiosInstance.put(`/workshops/${workshop.value.id}`, {
            ...workshop.value,
            startTime: formatToLocalISOString(startDate),
            endTime: formatToLocalISOString(endDate),
            trainer: selectedTrainerId.value ? { id: selectedTrainerId.value } : null,
            exchangeDay: selectedExchangeDayId.value ? { id: selectedExchangeDayId.value } : null
        });

        showToast(new Toast(
            "Erfolg",
            "Workshop wurde erfolgreich aktualisiert",
            "success",
            faCheck
        ));
        router.push(`/workshop/${workshop.value.id}`);
    } catch (err) {
        console.error('Error updating workshop:', err);
        showToast(new Toast(
            "Fehler",
            "Workshop konnte nicht aktualisiert werden",
            "error",
            faExclamation
        ));
    } finally {
        isLoading.value = false;
    }
}

// --- Formularvalidierung ---
/**
 * Validiert das Formular vor dem Absenden
 */
function validateForm(): boolean {
    if (!workshop.value.title.trim()) {
        showToast(new Toast("Fehler", "Bitte geben Sie einen Titel ein", "error", faExclamation));
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

    // Validierung des Zeitraums wenn ein Exchange Day ausgewählt ist
    if (selectedExchangeDayId.value) {
        const selectedExchangeDay = exchangeDays.value.find(ed => ed.id === selectedExchangeDayId.value);
        if (selectedExchangeDay) {
            const workshopStart = new Date(workshop.value.startTime);
            const workshopEnd = new Date(workshop.value.endTime);
            const exchangeDayStart = new Date(selectedExchangeDay.startTime);
            const exchangeDayEnd = new Date(selectedExchangeDay.endTime);

            if (workshopStart < exchangeDayStart || workshopEnd > exchangeDayEnd) {
                showToast(new Toast(
                    "Fehler",
                    `Der Workshop muss im Zeitraum des Exchange Days stattfinden (${
                        exchangeDayStart.toLocaleDateString('de-DE')
                    } bis ${
                        exchangeDayEnd.toLocaleDateString('de-DE')
                    })`,
                    "error",
                    faExclamation
                ));
                return false;
            }
        }
    }

    if (new Date(workshop.value.startTime) >= new Date(workshop.value.endTime)) {
        showToast(new Toast("Fehler", "Die Endzeit muss nach der Startzeit liegen", "error", faExclamation));
        return false;
    }
    if (!workshop.value.location.trim()) {
        showToast(new Toast("Fehler", "Bitte geben Sie einen Ort an", "error", faExclamation));
        return false;
    }
    if (workshop.value.maxParticipants < 1) {
        showToast(new Toast("Fehler", "Die maximale Teilnehmerzahl muss mindestens 1 sein", "error", faExclamation));
        return false;
    }
    return true;
}

// --- Navigation ---
function goBack() {
    router.push(`/workshop/${workshop.value.id}`)
}

// --- Watchers ---
// Beobachtet Änderungen am ausgewählten Exchange Day
watch(() => selectedExchangeDayId.value, (newId) => {
    if (newId) {
        const selectedExchangeDay = exchangeDays.value.find(ed => ed.id === newId);
        if (selectedExchangeDay) {
            workshop.value.exchangeDay = selectedExchangeDay;
        }
    } else {
        workshop.value.exchangeDay = null;
    }
});

// Beobachtet Änderungen am Workshop
watch(() => workshop.value, (newWorkshop) => {
    if (newWorkshop.exchangeDay && !selectedExchangeDayId.value) {
        selectedExchangeDayId.value = newWorkshop.exchangeDay.id;
    }
}, { deep: true, immediate: true });

// --- Lifecycle Hooks ---
onMounted(async () => {
    try {
        // Lade zuerst Exchange Days und Trainer
        await Promise.all([fetchExchangeDays(), fetchTrainers()]);
        // Dann den Workshop
        await fetchWorkshop();
    } catch (error) {
        console.error('Fehler beim Laden der Daten:', error);
    }
});
</script>

<template>
    <div class="page-container">
        <div class="form-container">
            <!-- Loading State -->
            <div v-if="isLoading" class="loading">
                Lädt...
            </div>

            <template v-else>
                <!-- Header -->
                <div class="form-header">
                    <h1>Workshop bearbeiten</h1>
                    <Button class="back-btn" @click="goBack">Zurück</Button>
                </div>

                <!-- Hauptformular -->
                <form @submit.prevent="updateWorkshop" class="workshop-form">
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
                        <label for="trainer">Trainer</label>
                        <select
                            id="trainer"
                            v-model="selectedTrainerId"
                            class="form-control"
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
                                {{ exchangeDay.title }} ({{ new Date(exchangeDay.startTime).toLocaleDateString('de-DE') }} -
                                {{ new Date(exchangeDay.endTime).toLocaleDateString('de-DE') }})
                            </option>
                        </select>
                    </div>

                    <!-- Zeit- und Ortsangaben -->
                    <div class="form-row">
                        <div class="form-group">
                            <label for="startTime">Startzeit<span class="required">*</span></label>
                            <input
                                id="startTime"
                                v-model="workshop.startTime"
                                type="datetime-local"
                                required
                                class="calendar-input"
                                :min="selectedExchangeDayId ? exchangeDays.find(ed => ed.id === selectedExchangeDayId)?.startTime : undefined"
                                :max="selectedExchangeDayId ? exchangeDays.find(ed => ed.id === selectedExchangeDayId)?.endTime : undefined"
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
                                :max="selectedExchangeDayId ? exchangeDays.find(ed => ed.id === selectedExchangeDayId)?.endTime : undefined"
                            >
                        </div>
                    </div>

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

                    <!-- Submit Button -->
                    <div class="form-actions">
                        <Button
                            type="submit"
                            class="submit-btn"
                            :disabled="isLoading"
                        >
                            {{ isLoading ? 'Wird gespeichert...' : 'Änderungen speichern' }}
                        </Button>
                    </div>
                </form>
            </template>
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

/* --- Status Styles --- */
.loading {
    text-align: center;
    padding: 2rem;
    font-size: 1.2rem;
    color: #2c3e50;
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
}

input, textarea, select {
    padding: 0.75rem;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 1rem;
    transition: all 0.3s ease;
    background-color: #f8f9fa;
    color: #2c3e50;
}

input:focus, textarea:focus, select:focus {
    outline: none;
    border-color: #409eff;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

textarea {
    resize: vertical;
    min-height: 120px;
    max-height: 300px;
}

.required {
    color: #e74c3c;
    margin-left: 4px;
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

/* --- Button Styles --- */
:deep(.back-btn) {
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

:deep(.back-btn:hover) {
    background-color: #2980b9 !important;
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
}

:deep(.submit-btn:hover) {
    background-color: #219a52;
}

:deep(.submit-btn:disabled) {
    background-color: #a0aec0;
    cursor: not-allowed;
}

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

    .form-header {
        flex-direction: column;
        gap: 1rem;
    }

    .form-header h1 {
        font-size: 2rem;
    }
}
</style>
