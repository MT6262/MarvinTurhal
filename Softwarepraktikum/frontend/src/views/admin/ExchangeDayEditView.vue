<!--ExchangeDayEditView-->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { Button } from "agnostic-vue"
import { Toast, showToast } from "@/ts/toasts"
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import config from '@/config'

// --- Interfaces ---
/**
 * Definiert ein Exchange Day-Objekt
 */
interface ExchangeDay {
    id: number;
    title: string;
    startTime: string;
    endTime: string;
    description: string;
}

// --- State Management ---
const exchangeDay = ref<ExchangeDay>({
    id: 0,
    title: '',
    startTime: '',
    endTime: '',
    description: ''
})

const route = useRoute()
const router = useRouter()
const isLoading = ref(false)

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
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day}T${hours}:${minutes}`
}

// --- API Funktionen ---
/**
 * Lädt den zu bearbeitenden Exchange Day
 */
async function fetchExchangeDay() {
    isLoading.value = true
    try {
        const response = await axiosInstance.get(`/exchangedays/${route.params.id}`)
        const data = response.data
        exchangeDay.value = {
            ...data,
            startTime: formatToLocalISOString(new Date(data.startTime)),
            endTime: formatToLocalISOString(new Date(data.endTime))
        }
    } catch (err) {
        showToast(new Toast(
            "Fehler",
            "Exchange Day konnte nicht geladen werden",
            "error",
            faExclamation
        ))
    } finally {
        isLoading.value = false
    }
}

/**
 * Aktualisiert den Exchange Day
 */
async function updateExchangeDay() {
    if (!validateForm()) return

    isLoading.value = true
    try {
        await axiosInstance.put(`/exchangedays/${exchangeDay.value.id}`, {
            ...exchangeDay.value,
            startTime: new Date(exchangeDay.value.startTime).toISOString(),
            endTime: new Date(exchangeDay.value.endTime).toISOString()
        })
        showToast(new Toast(
            "Erfolg",
            "Exchange Day wurde aktualisiert",
            "success",
            faCheck
        ))
        router.push('/admin')
    } catch (err) {
        showToast(new Toast(
            "Fehler",
            "Exchange Day konnte nicht aktualisiert werden",
            "error",
            faExclamation
        ))
    } finally {
        isLoading.value = false
    }
}

// --- Formular Funktionen ---
/**
 * Validiert die Formulareingaben
 */
function validateForm(): boolean {
    if (!exchangeDay.value.title.trim()) {
        showToast(new Toast(
            "Fehler",
            "Bitte geben Sie einen Titel ein",
            "error",
            faExclamation
        ))
        return false
    }
    if (new Date(exchangeDay.value.startTime) >= new Date(exchangeDay.value.endTime)) {
        showToast(new Toast(
            "Fehler",
            "Die Endzeit muss nach der Startzeit liegen",
            "error",
            faExclamation
        ))
        return false
    }
    return true
}

// --- Navigation ---
function goBack() {
    router.push('/admin')
}

// --- Lifecycle Hooks ---
onMounted(fetchExchangeDay)
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
                    <h1>Exchange Day bearbeiten</h1>
                    <Button class="back-btn" @click="goBack">Zurück</Button>
                </div>

                <!-- Hauptformular -->
                <form @submit.prevent="updateExchangeDay" class="exchangeday-form">
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

                    <!-- Zeitauswahl -->
                    <div class="form-row">
                        <div class="form-group">
                            <label for="startTime">Startzeit<span class="required">*</span></label>
                            <input
                                id="startTime"
                                v-model="exchangeDay.startTime"
                                type="datetime-local"
                                required
                                class="calendar-input"
                            >
                        </div>

                        <div class="form-group">
                            <label for="endTime">Endzeit<span class="required">*</span></label>
                            <input
                                id="endTime"
                                v-model="exchangeDay.endTime"
                                type="datetime-local"
                                required
                                class="calendar-input"
                                :min="exchangeDay.startTime"
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

/* --- Loading State Styles --- */
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
.exchangeday-form {
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

input {
    padding: 0.75rem;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 1rem;
    transition: all 0.3s ease;
    background-color: #f8f9fa;
    color: #2c3e50;
}

input:focus {
    outline: none;
    border-color: #409eff;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* --- Calendar Input Styles --- */
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

/* --- Button Styles --- */
:deep(.back-btn) {
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

:deep(.back-btn:hover) {
    background-color: #2980b9;
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

/* --- Utility Styles --- */
.required {
    color: #e74c3c;
    margin-left: 4px;
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
