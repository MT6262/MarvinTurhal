<!--WorkshopDetails-->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button } from "agnostic-vue"
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import axios from 'axios'
import { Toast, showToast } from "@/ts/toasts"
import config from '@/config';

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
    trainer?: {
        id: number;
        firstName: string;
        lastName: string;
        email: string;
    };
    exchangeDay?: {
        id: number;
        title: string;
        startTime: string;
        endTime: string;
    };
}

// --- State Management ---
const route = useRoute()
const router = useRouter()
const workshop = ref<Workshop | null>(null)
const isLoading = ref(false)
const error = ref<string | null>(null)

// --- API Konfiguration ---
const axiosInstance = axios.create({
    baseURL: config.API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

// --- API Funktionen ---
/**
 * Lädt die Details eines Workshops vom Server
 */
async function fetchWorkshopDetails() {
    const workshopId = route.params.id
    isLoading.value = true
    error.value = null

    try {
        const response = await axiosInstance.get(`/workshops/${workshopId}`)
        workshop.value = response.data
    } catch (err) {
        console.error('Error fetching workshop details:', err)
        error.value = 'Workshop konnte nicht geladen werden'
        showToast(new Toast(
            "Fehler",
            "Workshop-Details konnten nicht geladen werden",
            "error",
            faExclamation
        ))
    } finally {
        isLoading.value = false
    }
}

/**
 * Löscht einen Workshop nach Bestätigung
 */
async function deleteWorkshop() {
    if (!confirm('Möchten Sie diesen Workshop wirklich löschen?')) return

    isLoading.value = true
    try {
        await axiosInstance.delete(`/workshops/${workshop.value?.id}`)
        showToast(new Toast(
            "Erfolg",
            "Workshop wurde erfolgreich gelöscht",
            "success",
            faCheck
        ))
        router.push('/admin')
    } catch (err) {
        console.error('Error deleting workshop:', err)
        showToast(new Toast(
            "Fehler",
            "Workshop konnte nicht gelöscht werden",
            "error",
            faExclamation
        ))
    } finally {
        isLoading.value = false
    }
}

// --- Hilfsfunktionen ---
/**
 * Formatiert ein Datum in lokales Format
 */
function formatDateTime(dateString: string): string {
    return new Date(dateString).toLocaleString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    })
}

// --- Navigation ---
function goToEdit() {
    router.push(`/workshop/${workshop.value?.id}/edit`)
}

function goBack() {
    router.push('/admin')
}

// --- Lifecycle Hooks ---
onMounted(() => {
    fetchWorkshopDetails()
})
</script>

<template>
    <div class="page-container">
        <div class="details-container">
            <!-- Loading und Error States -->
            <div v-if="isLoading" class="loading">
                Lädt...
            </div>

            <div v-else-if="error" class="error">
                {{ error }}
            </div>

            <!-- Workshop Details -->
            <div v-else-if="workshop" class="workshop-details">
                <!-- Header mit Aktions-Buttons -->
                <div class="header">
                    <h1>{{ workshop.title }}</h1>
                    <div class="button-group">
                        <Button class="edit-btn" @click="goToEdit">
                            Bearbeiten
                        </Button>
                        <Button class="delete-btn" @click="deleteWorkshop">
                            Löschen
                        </Button>
                        <Button class="back-btn" @click="goBack">
                            Zurück
                        </Button>
                    </div>
                </div>

                <!-- Hauptinhalt -->
                <div class="content">
                    <!-- Informations-Grid -->
                    <div class="info-section">
                        <h2>Details</h2>
                        <div class="info-grid">
                            <div class="info-item">
                                <label>Startzeit:</label>
                                <span>{{ formatDateTime(workshop.startTime) }}</span>
                            </div>
                            <div class="info-item">
                                <label>Endzeit:</label>
                                <span>{{ formatDateTime(workshop.endTime) }}</span>
                            </div>
                            <div class="info-item">
                                <label>Ort:</label>
                                <span>{{ workshop.location }}</span>
                            </div>
                            <div class="info-item">
                                <label>Teilnehmer:</label>
                                <span>{{ workshop.currentParticipants }} / {{ workshop.maxParticipants }}</span>
                            </div>
                            <div class="info-item" v-if="workshop.trainer">
                                <label>Trainer:</label>
                                <span>{{ workshop.trainer.firstName }} {{ workshop.trainer.lastName }}</span>
                            </div>
                            <div class="info-item" v-if="workshop.exchangeDay">
                                <label>Exchange Day:</label>
                                <span>{{ workshop.exchangeDay.title }} ({{ formatDateTime(workshop.exchangeDay.startTime) }} - {{ formatDateTime(workshop.exchangeDay.endTime) }})</span>
                            </div>
                        </div>
                    </div>

                    <!-- Beschreibung -->
                    <div class="description-section" v-if="workshop.description">
                        <h2>Beschreibung</h2>
                        <p>{{ workshop.description }}</p>
                    </div>
                </div>
            </div>

            <div v-else class="error">
                Workshop nicht gefunden
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

.details-container {
    max-width: 800px;
    margin: 0 auto;
    padding: 2rem;
    background: white;
    border-radius: 16px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

/* --- Status Styles --- */
.loading, .error {
    text-align: center;
    padding: 2rem;
    font-size: 1.2rem;
    color: #2c3e50;
}

.error {
    color: #e74c3c;
}

/* --- Header Styles --- */
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid #edf2f7;
}

.header h1 {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.5rem;
    font-size: 2.5rem;
    font-weight: bold;
    color: #2c3e50;
    text-align: left;
    line-height: 1.2;
}

/* --- Button Styles --- */
.button-group {
    display: flex;
    gap: 1rem;
}

:deep(.edit-btn) {
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

:deep(.edit-btn:hover) {
    background-color: #219a52;
}

:deep(.delete-btn) {
    padding: 0.75rem 1.5rem;
    background-color: #e74c3c;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
}

:deep(.delete-btn:hover) {
    background-color: #c0392b;
}

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

/* --- Content Layout Styles --- */
.content {
    display: flex;
    flex-direction: column;
    gap: 2rem;
}

.info-section, .description-section {
    background-color: #f8fafc;
    padding: 1.5rem;
    border-radius: 8px;
}

.info-section h2, .description-section h2 {
    color: #2c3e50;
    font-size: 1.5rem;
    margin-bottom: 1rem;
}

/* --- Info Grid Styles --- */
.info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1rem;
}

.info-item {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.info-item label {
    font-weight: 600;
    color: #64748b;
}

.info-item span {
    color: #2c3e50;
}

/* --- Description Styles --- */
.description-section p {
    color: #2c3e50;
    line-height: 1.6;
    white-space: pre-line;
}

@media (max-width: 768px) {
    .page-container {
        padding: 1rem;
    }

    .details-container {
        padding: 1rem;
    }

    .header {
        flex-direction: column;
        gap: 1rem;
        align-items: flex-start;
    }

    .button-group {
        width: 100%;
        justify-content: flex-start;
    }
}
</style>
