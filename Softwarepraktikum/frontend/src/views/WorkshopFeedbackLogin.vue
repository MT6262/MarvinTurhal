<!--WorkshopFeedbackLogin-->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Button } from "agnostic-vue"
import { showToast, Toast } from "@/ts/toasts"
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import config from "@/config"

// --- Router und State Setup ---
const route = useRoute()
const router = useRouter()

// Login Formular State
const email = ref("")
const password = ref("")

/**
 * Behandelt den Login-Prozess
 * Sendet die Email an den Server und speichert die Mitarbeiterdaten im localStorage
 */
async function handleLogin() {
    try {
        const response = await axios.create({
            baseURL: config.API_BASE_URL
        }).post('/login', {
            email: email.value
        });

        if (response.data) {
            // Mitarbeiterdaten im localStorage speichern
            const employee = response.data;
            localStorage.setItem('employeeId', employee.id.toString());
            localStorage.setItem('employeeFirstName', employee.firstName);
            localStorage.setItem('employeeLastName', employee.lastName);
            localStorage.setItem('sessionStartTime', new Date().getTime().toString());

            // Weiterleitung zur Feedback-Seite
            router.replace(`/qr-feedback/${route.params.workshopId}`);
        }
    } catch (error) {
        showToast(new Toast(
            "Login fehlgeschlagen",
            "Diese Email-Adresse ist nicht registriert",
            "error",
            faExclamation
        ));
    }
}
</script>

<!-- Template-Struktur -->
<template>
    <!-- Hauptcontainer für Login -->
    <main class="login-container">
        <!-- Login Box mit Formular -->
        <div class="login-box">
            <!-- Überschriften -->
            <h1 class="main-title">Itestra <br>Event Kalender</h1>
            <h2 class="subtitle">Melden Sie sich hier an um ihr Feedback abzusenden:</h2>

            <!-- Toggle für Benutzerauswahl (aktuell statisch) -->
            <div class="toggle-container">
                <button class="toggle-button active">
                    Benutzer
                </button>
            </div>

            <!-- Email Eingabefeld -->
            <div class="form-group">
                <label>E-Mail</label>
                <input
                    type="email"
                    v-model="email"
                    class="form-control"
                    placeholder="E-Mail eingeben"
                    required
                />
            </div>

            <!-- Passwort Eingabefeld (gemockt) -->
            <div class="form-group">
                <label>Passwort(mocked)</label>
                <input
                    type="password"
                    v-model="password"
                    class="form-control"
                    placeholder="Passwort eingeben"
                />
            </div>

            <!-- Login und Register Buttons -->
            <Button
                class="login-button"
                @click="handleLogin"
            >
                Anmelden
            </Button>
            <Button
                class="register-button"
                @click="router.replace('/register')"
            >
                Registrieren
            </Button>
        </div>
    </main>
</template>

<style scoped>
/* --- Container Styles --- */
.login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    padding: 1.5rem;
}

.login-box {
    background: white;
    padding: 2.5rem;
    border-radius: 16px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
    width: 100%;
    max-width: 400px;
}

/* --- Überschrift Styles --- */
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

/* --- Toggle Button Styles --- */
.toggle-container {
    display: flex;
    justify-content: center;
    margin-bottom: 2rem;
    padding: 0.3rem;
    background: #f8f9fa;
    border-radius: 8px;
}

.toggle-button {
    padding: 0.75rem 1.5rem;
    border: none;
    background-color: #3498db;
    color: white;
    font-weight: 500;
    border-radius: 6px;
    width: 100%;
    box-shadow: 0 2px 8px rgba(52, 152, 219, 0.3);
}

/* --- Formular Styles --- */
.form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
}

.form-control {
    padding: 0.75rem;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 1rem;
    transition: all 0.3s ease;
    background-color: #f8f9fa;
    color: #2c3e50;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    width: 100%;
}

/* --- Button Styles --- */
:deep(.login-button) {
    padding: 0.75rem 1.5rem;
    background-color: #3498db;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    width: 100%;
}

:deep(.register-button) {
    padding: 0.75rem 1.5rem;
    background-color: #27ae60;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
    width: 100%;
    margin-top: 1rem;
}

/* --- State Styles --- */
.toggle-container.disabled {
    opacity: 0.5;
    pointer-events: none;
}

.form-control:focus {
    outline: none;
    border-color: #409eff;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* --- Hover States --- */
.toggle-button:hover {
    color: #3498db;
    background-color: #f1f3f5;
}

.toggle-button.active:hover {
    background-color: #2980b9;
    color: white;
}

:deep(.login-button:hover) {
    background-color: #2980b9;
}

:deep(.register-button:hover) {
    background-color: #219a52;
}

/* --- Disabled States --- */
:deep(.login-button:disabled) {
    background-color: #a0aec0;
    cursor: not-allowed;
}
</style>
