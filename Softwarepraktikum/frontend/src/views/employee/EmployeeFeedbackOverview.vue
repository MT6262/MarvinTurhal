<!--EmployeeFeedbackOverview-->
<script setup lang="ts">
// --- Imports ---
// Vue Core & Router
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

// UI Komponenten und Icons
import { Button } from "agnostic-vue";
import { faExclamation } from '@fortawesome/free-solid-svg-icons';

// Utility Imports
import { Toast, showToast } from "@/ts/toasts";
import config from '@/config';

// --- Interfaces ---
/**
 * Definiert die Struktur der Feedback-Daten
 */
interface FeedbackData {
    id: number;
    workshopID: number;
    employeeID: number;
    rating: number;
    comment: string;
    isAnonymous: boolean;
    anonymousToken: string | null;
    timestamp: string;
}

// --- State Management ---
const router = useRouter();
const myFeedbackList = ref<FeedbackData[]>([]);
const isLoading = ref(true);
const workshopTitles = ref<{ [key: number]: string }>({});

// --- Daten-Lade-Funktionen ---
/**
 * Lädt das Feedback des eingeloggten Mitarbeiters
 */
async function loadMyFeedback() {
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

        const response = await fetch(`${config.API_BASE_URL}/feedback/employees/${employeeId}`);
        if (!response.ok) {
            throw new Error('Backend-Fehler: ' + response.statusText);
        }

        const data = await response.json();
        myFeedbackList.value = data;

        // Lade Workshop-Titel für jedes Feedback
        await Promise.all(data.map(async (feedback: FeedbackData) => {
            const workshopResponse = await fetch(`${config.API_BASE_URL}/workshops/${feedback.workshopID}`);
            if (workshopResponse.ok) {
                const workshop = await workshopResponse.json();
                workshopTitles.value[feedback.workshopID] = workshop.title;
            }
        }));

    } catch (err) {
        console.error('Fehler beim Laden des Feedbacks:', err);
        showToast(new Toast(
            "Fehler",
            "Feedback konnte nicht geladen werden",
            "error",
            faExclamation
        ));
    } finally {
        isLoading.value = false;
    }
}

// --- Lifecycle Hooks ---
onMounted(loadMyFeedback);
</script>

<template>
    <main class="feedback-container">
        <!-- Header -->
        <div class="header">
            <h1>Mein Feedback</h1>
            <Button class="back-button" @click="router.push('/user')">Zurück</Button>
        </div>

        <!-- Loading und Empty States -->
        <div v-if="isLoading" class="loading">
            Lade Feedback...
        </div>

        <div v-else-if="myFeedbackList.length === 0" class="no-feedback">
            Sie haben noch kein Feedback abgegeben.
        </div>

        <!-- Feedback Liste -->
        <div v-else class="feedback-list">
            <div v-for="feedback in myFeedbackList"
                 :key="feedback.id"
                 class="feedback-card">
                <!-- Feedback Header -->
                <div class="feedback-header">
                    <h3>{{ workshopTitles[feedback.workshopID] || 'Workshop lädt...' }}</h3>
                    <span class="feedback-date">
                        {{ new Date(feedback.timestamp).toLocaleDateString('de-DE') }}
                    </span>
                </div>

                <!-- Feedback Inhalt -->
                <div class="feedback-content">
                    <div class="rating">
                        Bewertung: {{ feedback.rating }}/5
                    </div>
                    <div class="comment">
                        <strong>Ihr Feedback zum Workshop:</strong>
                        <p>{{ feedback.comment }}</p>
                        <span v-if="feedback.isAnonymous" class="anonymous-badge">
                            Anonym abgegeben
                        </span>
                    </div>
                </div>
            </div>
        </div>
    </main>
</template>

<style scoped>
/* --- Basis Layout Styles --- */
.feedback-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    padding: 2rem;
}

/* --- Header Styles --- */
.header {
    max-width: 1200px;
    margin: 0 auto 2rem auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 1rem;
}

.header h1 {
    color: #2c3e50;
    font-size: 2rem;
    font-weight: 700;
    margin: 0;
}

/* --- Button Styles --- */
:deep(.header button) {
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

:deep(.header button:hover) {
    background-color: #219a52;
}

.back-button {
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

.back-button:hover {
    background-color: #2980b9 !important;
}

/* --- Feedback List Styles --- */
.feedback-list {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 1rem;
}

/* --- Feedback Card Styles --- */
.feedback-card {
    background: white;
    border-radius: 16px;
    padding: 1.5rem;
    margin-bottom: 1.5rem;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.feedback-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    padding-bottom: 1rem;
    border-bottom: 1px solid #e9ecef;
}

.feedback-header h3 {
    color: #2c3e50;
    font-size: 1.4rem;
    font-weight: 600;
    margin: 0;
}

.feedback-date {
    color: #7f8c8d;
    font-size: 0.9rem;
}

/* --- Content Styles --- */
.feedback-content {
    color: #2c3e50;
}

.rating {
    font-size: 1.1rem;
    font-weight: 500;
    margin-bottom: 1rem;
}

.comment {
    margin: 1rem 0;
    line-height: 1.6;
}

.comment strong {
    display: block;
    margin-bottom: 0.5rem;
}

/* --- Utility Styles --- */
.anonymous-badge {
    display: inline-block;
    background: #f8f9fa;
    padding: 0.25rem 0.75rem;
    border-radius: 4px;
    font-size: 0.875rem;
    color: #7f8c8d;
    margin-top: 0.5rem;
}

.loading, .no-feedback {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
    background: white;
    border-radius: 16px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
    text-align: center;
    color: #2c3e50;
    font-size: 1.1rem;
}
</style>
