<!--FeedbackView-->
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Button } from "agnostic-vue";
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons';
import axios from 'axios';
import { Toast, showToast } from "@/ts/toasts";
import config from "@/config";

// --- Interfaces ---
/**
 * Definiert ein Workshop-Objekt für das Feedback
 */
interface Workshop {
    id: number;
    title: string;
    description: string;
    trainer?: {
        id: number;
        firstName: string;
        lastName: string;
    };
}

// --- State Management ---
const route = useRoute();
const router = useRouter();
const isLoading = ref<boolean>(false);
const feedbackList = ref<any[]>([]);

// Workshop-Zustand
const workshop = ref<Workshop | null>(null);

// Feedback-Formular-Zustand
const rating = ref<number>(0);
const comment = ref<string>('');
const trainerRating = ref<number>(0);
const trainerComment = ref<string>('');
const isAnonymous = ref<boolean>(false);
const isSubmitting = ref<boolean>(false);

// --- API Konfiguration ---
const axiosInstance = axios.create({
    baseURL: config.API_BASE_URL,
});

// --- Lifecycle Hooks ---
onMounted(async () => {
    const workshopId = route.params.workshopId;
    if (!workshopId) {
        showToast(new Toast(
            "Fehler",
            "Kein Workshop ausgewählt",
            "error",
            faExclamation
        ));
        router.push('/user');
        return;
    }

    try {
        // Lade Workshop-Details
        const response = await axios.get(`${config.API_BASE_URL}/workshops/${workshopId}`);
        workshop.value = response.data;
    } catch (err) {
        console.error("Fehler beim Laden des Workshops:", err);
        showToast(new Toast(
            "Fehler",
            "Workshop konnte nicht geladen werden",
            "error",
            faExclamation
        ));
        router.push('/user');
    }
});

// --- Formular Funktionen ---
/**
 * Sendet das Workshop-Feedback ab
 * Prüft Pflichtfelder und erstellt Feedback
 */
const submitFeedback = async () => {
    // Validierung der Workshop-Bewertung
    if (rating.value === 0) {
        showToast(new Toast(
            "Fehler",
            "Bitte geben Sie eine Workshop-Bewertung ab",
            "error",
            faExclamation
        ));
        return;
    }

    // Validierung der Trainer-Bewertung falls Trainer vorhanden
    if (workshop.value?.trainer && trainerRating.value === 0) {
        showToast(new Toast(
            "Fehler",
            "Bitte geben Sie auch eine Trainer-Bewertung ab",
            "error",
            faExclamation
        ));
        return;
    }

    isSubmitting.value = true;

    try {
        // Prüfe Login-Status
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

        // Bereite Feedback-Daten vor
        const feedbackData = {
            workshop: {
                id: workshop.value?.id
            },
            employee: {
                id: parseInt(employeeId)
            },
            rating: rating.value,
            comment: comment.value.trim(),
            trainerRating: workshop.value?.trainer ? trainerRating.value : null,
            trainerComment: workshop.value?.trainer ? trainerComment.value.trim() : null,
            anonymous: isAnonymous.value,
            timestamp: new Date()
        };

        // Sende Feedback an Server
        await axios.post(`${config.API_BASE_URL}/feedback`, feedbackData);

        showToast(new Toast(
            "Erfolg",
            "Vielen Dank für Ihr Feedback!",
            "success",
            faCheck
        ));

        router.push('/user');
    } catch (err: any) {
        console.error("Fehler beim Speichern des Feedbacks:", err);

        // Spezielle Fehlerbehandlung für doppelte Feedbacks
        let errorMessage = "Feedback konnte nicht gespeichert werden";
        if (axios.isAxiosError(err)) {
            if (err.response?.status === 409) {
                errorMessage = "Sie haben bereits Feedback zu diesem Workshop abgegeben";
            }
        }

        showToast(new Toast(
            "Fehler",
            errorMessage,
            "error",
            faExclamation
        ));
    } finally {
        isSubmitting.value = false;
    }
};

// --- Daten-Lade-Funktionen ---
/**
 * Lädt Workshops und zugehörige Feedbacks
 */
async function loadData() {
    try {
        const [workshopsResponse, feedbackResponse] = await Promise.all([
            axiosInstance.get('/workshops'),
            axiosInstance.get('/feedback')
        ]);

        workshop.value = workshopsResponse.data;
        feedbackList.value = feedbackResponse.data;
    } catch (err) {
        console.error('Error loading data:', err);
        showToast(new Toast(
            "Fehler",
            "Daten konnten nicht geladen werden",
            "error",
            faExclamation
        ));
    } finally {
        isLoading.value = false;
    }
}
</script>

<template>
    <main class="feedback-container">
        <div class="feedback-box">
            <!-- Feedback-Formular -->
            <div v-if="workshop" class="feedback-form">
                <h1 class="main-title">Workshop Feedback</h1>
                <h2 class="subtitle">{{ workshop.title }}</h2>

                <!-- Workshop-Beschreibung -->
                <div class="form-group">
                    <div class="workshop-info">
                        <p>{{ workshop.description }}</p>
                    </div>
                </div>

                <!-- Workshop-Bewertung -->
                <div class="form-group">
                    <label>Workshop-Bewertung</label>
                    <div class="star-rating">
                        <button
                            v-for="star in 5"
                            :key="star"
                            class="star-btn"
                            :class="{ active: star <= rating }"
                            @click="rating = star"
                            type="button"
                        >
                            ★
                        </button>
                    </div>
                </div>

                <!-- Workshop-Kommentar -->
                <div class="form-group">
                    <label>Workshop-Kommentar</label>
                    <textarea
                        v-model="comment"
                        class="form-control"
                        placeholder="Was hat Ihnen am Workshop besonders gefallen? Was können wir verbessern?"
                        rows="4"
                    ></textarea>
                </div>

                <!-- Trainer-Bewertung falls Trainer vorhanden -->
                <div class="form-group" v-if="workshop.trainer">
                    <label>Trainer-Bewertung ({{ workshop.trainer.firstName }} {{ workshop.trainer.lastName }})</label>
                    <div class="star-rating">
                        <button
                            v-for="star in 5"
                            :key="'trainer-' + star"
                            class="star-btn"
                            :class="{ active: star <= trainerRating }"
                            @click="trainerRating = star"
                            type="button"
                        >
                            ★
                        </button>
                    </div>
                </div>

                <!-- Trainer-Kommentar falls Trainer vorhanden -->
                <div class="form-group" v-if="workshop.trainer">
                    <label>Trainer-Feedback</label>
                    <textarea
                        v-model="trainerComment"
                        class="form-control"
                        placeholder="Wie war der Trainer? Was hat Ihnen besonders gefallen?"
                        rows="4"
                    ></textarea>
                </div>

                <!-- Anonymitäts-Option -->
                <div class="form-group checkbox-group">
                    <label class="checkbox-container">
                        <input
                            type="checkbox"
                            v-model="isAnonymous"
                        >
                        Anonym einreichen
                    </label>
                </div>

                <!-- Action-Buttons -->
                <div class="button-group">
                    <Button
                        class="cancel-button"
                        @click="router.push('/user')"
                    >
                        Abbrechen
                    </Button>
                    <Button
                        class="submit-button"
                        @click="submitFeedback"
                        :disabled="isSubmitting"
                    >
                        {{ isSubmitting ? 'Wird gesendet...' : 'Feedback senden' }}
                    </Button>
                </div>
            </div>
        </div>
    </main>
</template>

<style scoped>
/* --- Basis Layout Styles --- */
.feedback-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    padding: 1.5rem;
}

.feedback-box {
    background: white;
    padding: 2.5rem;
    border-radius: 16px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
    width: 100%;
    max-width: 600px;
}

/* --- Typography Styles --- */
.main-title {
    font-size: 2.5rem;
    font-weight: 700;
    color: #2c3e50;
    text-align: center;
    margin-bottom: 0.5rem;
    line-height: 1.2;
}

.subtitle {
    font-size: 1.1rem;
    color: #7f8c8d;
    text-align: center;
    margin-bottom: 2rem;
    font-weight: 500;
}

/* --- Form Styles --- */
.form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
}

.form-group label {
    font-weight: 500;
    color: #2c3e50;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.workshop-info {
    background-color: #f8f9fa;
    padding: 1rem;
    border-radius: 8px;
    margin-bottom: 1rem;
    color:#7f8c8d
}

/* --- Input Styles --- */
.form-control {
    padding: 0.75rem;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 1rem;
    transition: all 0.3s ease;
    background-color: #f8f9fa;
    color: #2c3e50;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    resize: vertical;
    min-height: 100px;
}

.form-control:focus {
    outline: none;
    border-color: #409eff;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* --- Star Rating Styles --- */
.star-rating {
    display: flex;
    gap: 0.5rem;
    padding: 0.5rem 0;
}

.star-btn {
    background: none;
    border: none;
    font-size: 2rem;
    color: #ddd;
    cursor: pointer;
    transition: color 0.3s ease;
    padding: 0;
}

.star-btn.active {
    color: #f1c40f;
}

/* --- Checkbox Styles --- */
.checkbox-group {
    margin-top: 1rem;
}

.checkbox-container {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    cursor: pointer;
}

.info-text {
    font-size: 0.9rem;
    color: #7f8c8d;
    margin-top: 0.5rem;
}

/* --- Button Styles --- */
.button-group {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    margin-top: 2rem;
}

:deep(.cancel-button) {
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

:deep(.cancel-button:hover) {
    background-color: #c0392b;
}

:deep(.submit-button) {
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

:deep(.submit-button:hover) {
    background-color: #219a52;
}

:deep(.submit-button:disabled) {
    background-color: #a0aec0;
    cursor: not-allowed;
}
</style>
