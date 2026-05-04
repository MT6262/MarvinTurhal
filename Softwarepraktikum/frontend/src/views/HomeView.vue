<!--HomeView.vue-->
<script setup lang="ts">
import {onMounted, ref} from 'vue'
import { useRoute } from 'vue-router'
import router from "@/router"
import { Button } from "agnostic-vue"
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import axios from 'axios'
import config from "@/config"
import { showToast, Toast } from "@/ts/toasts"

// Route-Information für spezielle Login-Fälle (z.B. Workshop-Feedback)
const route = useRoute()

// Formular-Zustand
const email = ref("")
const password = ref("")
const userType = ref("user")

// --- Hauptfunktionen ---
/**
 * Handlet den Login-Prozess
 * - Validiert Benutzerrechte
 * - Speichert Benutzerinformationen
 * - Zeigt Erfolgs-/Fehlermeldungen
 * - Leitet zu entsprechender Seite weiter
 */
async function handleLogin() {
    try {
        // API-Anfrage zum Login
        const response = await axios.create({
            baseURL: config.API_BASE_URL
        }).post('/login', {
            email: email.value
        });

        if (response.data) {
            const employee = response.data;

            // Berechtigungsprüfung für gewählten Login-Typ
            if ((userType.value === 'admin' && !employee.admin) ||
                (userType.value === 'user' && employee.admin)) {
                showToast(new Toast(
                    "Zugriff verweigert",
                    `Sie haben keine ${userType.value === 'admin' ? 'Administrator' : 'Benutzer'}-Rechte.`,
                    "error",
                    faExclamation
                ));
                return;
            }

            // Speichern der Benutzerdaten im localStorage
            localStorage.setItem('employeeId', employee.id.toString());
            localStorage.setItem('employeeFirstName', employee.firstName);
            localStorage.setItem('employeeLastName', employee.lastName);
            localStorage.setItem('isAdmin', employee.admin.toString());
            localStorage.setItem('sessionStartTime', new Date().getTime().toString());
            window.dispatchEvent(new Event('loginStateChanged'));

            // Erfolgsmeldung anzeigen
            const userTypeText = userType.value === 'admin' ? 'Administrator' : 'Benutzer';
            showToast(new Toast(
                "Login erfolgreich!",
                `Willkommen ${employee.firstName}! Sie sind als ${userTypeText} angemeldet.`,
                "success",
                faCheck
            ));

            // Weiterleitung nach Login basierend auf Benutzertyp und Route
            if (route.name === 'workshop-feedback-login') {
                router.replace(`/feedback/${route.params.workshopId}`);
            } else {
                if (userType.value === 'admin') {
                    router.replace('/admin')
                } else {
                    router.replace('/user')
                }
            }
        }
    } catch (error: any) {
        // Fehlerbehandlung mit benutzerfreundlichen Meldungen
        if (!email.value.trim()) {
            showToast(new Toast(
                "Email fehlt",
                "Bitte geben Sie eine Email-Adresse ein",
                "error",
                faExclamation
            ));
        } else {
            showToast(new Toast(
                "Login fehlgeschlagen",
                "Diese Email-Adresse ist nicht registriert",
                "error",
                faExclamation
            ));
        }
    }
}

/**
 * Setzt den Benutzertyp (Admin/User)
 * Wird deaktiviert für Workshop-Feedback-Login
 */
function setUserType(type: 'user' | 'admin') {
    if (route.name !== 'workshop-feedback-login') {
        userType.value = type
    }
}

onMounted(() => {
    // Spezielle Behandlung für Workshop-Feedback
    if (route.name === 'workshop-feedback-login') {
        const redirectAfterLogin = `/feedback/${route.params.workshopId}`;
        const redirectAfterFeedback = '/';
        localStorage.setItem('redirectAfterLogin', redirectAfterLogin);
        localStorage.setItem('redirectAfterFeedback', redirectAfterFeedback);
    }
});
</script>

<template>
    <!--
      Haupt-Login-Container
      Enthält das Login
    -->
    <main class="login-container">
        <div class="login-box">
            <!-- Header-Bereich -->
            <h1 class="main-title">Itestra <br>Event Kalender</h1>
            <h2 class="subtitle">Melden Sie sich hier an:</h2>

            <!-- Benutzertyp-Auswahl -->
            <div class="toggle-container" :class="{ 'disabled': route.name === 'workshop-feedback-login' }">
                <button
                    class="toggle-button"
                    :class="{ active: userType === 'user' }"
                    @click="setUserType('user')"
                >
                    Benutzer
                </button>
                <button
                    class="toggle-button"
                    :class="{ active: userType === 'admin' }"
                    @click="setUserType('admin')"
                >
                    Administrator
                </button>
            </div>

            <!-- Login-Formular -->
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

            <div class="form-group">
                <label>Passwort(mocked)</label>
                <input
                    type="password"
                    v-model="password"
                    class="form-control"
                    placeholder="Passwort eingeben"
                />
            </div>

            <!-- Action-Buttons -->
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
/* Container Styles */
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

/* Typography Styles */
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

/* Toggle Button Styles */
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
    background: transparent;
    color: #7f8c8d;
    cursor: pointer;
    transition: all 0.2s ease;
    width: 50%;
    font-weight: 500;
    border-radius: 6px;
}

.toggle-button:hover {
    color: #3498db;
    background-color: #f1f3f5;
}

.toggle-button.active {
    background-color: #3498db;
    color: white;
    box-shadow: 0 2px 8px rgba(52, 152, 219, 0.3);
}

.toggle-button.active:hover {
    background-color: #2980b9;
    color: white;
}

/* Form Styles */
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

.form-control:focus {
    outline: none;
    border-color: #409eff;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.form-control::placeholder {
    color: #a0aec0;
}

/* Button Styles */
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

:deep(.login-button:hover) {
    background-color: #2980b9;
}

:deep(.login-button:disabled) {
    background-color: #a0aec0;
    cursor: not-allowed;
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

:deep(.register-button:hover) {
    background-color: #219a52;
}

/* Utility Styles */
.toggle-container.disabled {
    opacity: 0.5;
    pointer-events: none;
}
</style>
