<!--CreateExchangeDayView.vue-->
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from "agnostic-vue"
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import axios from 'axios'
import { Toast, showToast } from "@/ts/toasts"
import config from '@/config'

// --- Interfaces ---
/**
 * Definiert die Exchange Day Formulardaten
 */
interface ExchangeDayData {
    title: string;
    description: string | null;
    startDate: string;
    endDate: string;
}

// --- State Management ---
const router = useRouter()
const isLoading = ref(false)
const error = ref<string | null>(null)

// Formular-Zustand
const exchangeDay = ref<ExchangeDayData>({
    title: '',
    description: '',
    startDate: '',
    endDate: ''
});

// --- API Konfiguration ---
const axiosInstance = axios.create({
    baseURL: config.API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

// --- Formular Funktionen ---
/**
 * Erstellt einen neuen Exchange Day
 * Validiert die Eingaben und sendet die Daten an die API
 */
async function createExchangeDay() {
    if (!validateForm()) {
        return;
    }

    isLoading.value = true;
    error.value = null;

    try {
        // Formatiere das Datum mit der entsprechenden Uhrzeit
        const startDateTime = `${exchangeDay.value.startDate}T00:00:00`;
        const endDateTime = `${exchangeDay.value.endDate}T23:59:59`;

        const exchangeDayData = {
            title: exchangeDay.value.title,
            description: exchangeDay.value.description || null,
            startTime: startDateTime,
            endTime: endDateTime
        };

        console.log('Sending data:', exchangeDayData);

        const response = await axiosInstance.post('/exchangedays', exchangeDayData);
        console.log('Response:', response);

        showToast(new Toast(
            "Erfolgreich",
            "Der Exchange Day wurde erfolgreich erstellt",
            "success",
            faCheck
        ));
        router.push('/admin');
    } catch (err: any) {
        console.error('Error details:', err);
        const errorMessage = err.response?.data?.message || "Der Exchange Day konnte nicht erstellt werden";
        showToast(new Toast(
            "Fehler",
            errorMessage,
            "error",
            faExclamation
        ));
    } finally {
        isLoading.value = false;
    }
}

/**
 * Validiert die Formulareingaben
 * Prüft auf erforderliche Felder und Datumslogik
 */
function validateForm(): boolean {
    if (!exchangeDay.value.title?.trim()) {
        showToast(new Toast("Fehler", "Bitte geben Sie einen Titel ein", "error", faExclamation))
        return false
    }
    if (!exchangeDay.value.startDate) {
        showToast(new Toast("Fehler", "Bitte wählen Sie ein Startdatum", "error", faExclamation))
        return false
    }
    if (!exchangeDay.value.endDate) {
        showToast(new Toast("Fehler", "Bitte wählen Sie ein Enddatum", "error", faExclamation))
        return false
    }
    // Prüfe ob das Enddatum nach dem Startdatum liegt
    if (exchangeDay.value.endDate < exchangeDay.value.startDate) {
        showToast(new Toast("Fehler", "Das Enddatum muss nach dem Startdatum liegen", "error", faExclamation))
        return false
    }
    return true
}

// --- Navigation ---
function goBack() {
    router.push('/admin')
}
</script>

<template>
    <div class="page-container">
        <div class="form-container">
            <!-- Header mit Titel und Zurück-Button -->
            <div class="form-header">
                <h1>Exchange Day erstellen</h1>
                <Button class="back-btn" @click="goBack">Zurück</Button>
            </div>

            <!-- Hauptformular -->
            <form @submit.prevent="createExchangeDay" class="workshop-form">
                <!-- Titel-Eingabe -->
                <div class="form-group">
                    <label for="title">Titel<span class="required">*</span></label>
                    <input
                        id="title"
                        v-model="exchangeDay.title"
                        type="text"
                        required
                        placeholder="Exchange Day Titel"
                    >
                </div>

                <!-- Beschreibungs-Eingabe -->
                <div class="form-group">
                    <label for="description">Beschreibung</label>
                    <textarea
                        id="description"
                        v-model="exchangeDay.description"
                        rows="4"
                        placeholder="Exchange Day Beschreibung"
                    ></textarea>
                </div>

                <!-- Datum-Eingaben -->
                <div class="form-group">
                    <label for="startDate">Startdatum<span class="required">*</span></label>
                    <input
                        id="startDate"
                        v-model="exchangeDay.startDate"
                        type="date"
                        required
                        class="calendar-input"
                    >
                </div>

                <div class="form-group">
                    <label for="endDate">Enddatum<span class="required">*</span></label>
                    <input
                        id="endDate"
                        v-model="exchangeDay.endDate"
                        type="date"
                        required
                        class="calendar-input"
                        :min="exchangeDay.startDate"
                    >
                </div>

                <!-- Formular-Aktionen -->
                <div class="form-actions">
                    <Button
                        type="submit"
                        class="submit-btn"
                        :disabled="isLoading"
                    >
                        {{ isLoading ? 'Wird erstellt...' : 'Exchange Day erstellen' }}
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

/* --- Form Styles --- */
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

/* Input & Textarea Styles */
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

/* Label Styles */
label {
    font-weight: 500;
    color: #2c3e50;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.required {
    color: #e74c3c;
    margin-left: 4px;
}

/* --- Calendar Input Styles --- */
.calendar-input {
    padding: 0.75rem;
    width: 100%;
    max-width: 200px;
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

/* --- Button Styles --- */
.form-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 1rem;
}

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

@media (max-width: 768px) {
    .page-container {
        padding: 1rem;
    }

    .form-container {
        padding: 1rem;
    }
}
</style>
