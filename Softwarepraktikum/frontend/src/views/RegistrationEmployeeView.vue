<!--RegistrationEmployeeView-->
<script setup lang="ts">
import { ref } from 'vue'
import { Button } from "agnostic-vue"
import { showToast, Toast } from "@/ts/toasts"
import router from "@/router"
import axios from 'axios'
import { faCheck, faExclamation } from '@fortawesome/free-solid-svg-icons'
import config from "@/config"

const firstName = ref("")
const lastName = ref("")
const email = ref("")
const isAdmin = ref(false)

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const isEmailValid = ref(true)

function validateEmail() {
    isEmailValid.value = emailRegex.test(email.value)
}
async function handleRegistration() {
    validateEmail()
    if (!isEmailValid.value) {
        showToast(new Toast(
            "Ungültige E-Mail",
            "Bitte geben Sie eine gültige E-Mail-Adresse ein.",
            "error",
            faExclamation
        ))
        return
    }
    try {
        const response = await axios.post(`${config.API_BASE_URL}/employees`, {
            firstName: firstName.value,
            lastName: lastName.value,
            email: email.value,
            admin: isAdmin.value
        })

        if (response.status === 201) {
            showToast(new Toast(
                "Registrierung erfolgreich!",
                "Ihr Account wurde erstellt. Sie können sich jetzt anmelden.",
                "success",
                faCheck
            ))
            router.push('/')
        }
    } catch (error: any) {
        if (error.response?.status === 409) {
            showToast(new Toast(
                "Registrierung fehlgeschlagen",
                "Diese E-Mail-Adresse wird bereits verwendet.",
                "error",
                faExclamation
            ))
        } else {
            showToast(new Toast(
                "Registrierung fehlgeschlagen",
                "Bitte versuchen Sie es später erneut.",
                "error",
                faExclamation
            ))
        }
    }
}

function goBack() {
    router.push('/')
}
</script>

<template>
    <main class="registration-container">
        <div class="registration-box">
            <h1 class="main-title">Itestra <br>Event Kalender</h1>
            <h2 class="subtitle">Neuen Mitarbeiter registrieren:</h2>

            <div class="form-group">
                <label>Vorname</label>
                <input
                    type="text"
                    v-model="firstName"
                    class="form-control"
                    placeholder="Vorname eingeben"
                />
            </div>

            <div class="form-group">
                <label>Nachname</label>
                <input
                    type="text"
                    v-model="lastName"
                    class="form-control"
                    placeholder="Nachname eingeben"
                />
            </div>

            <div class="form-group">
                <label>E-Mail</label>
                <input
                    type="email"
                    v-model="email"
                    class="form-control"
                    placeholder="E-Mail eingeben"
                />
            </div>

            <div class="form-group checkbox-group">
                <label class="checkbox-label">
                    <input
                        type="checkbox"
                        v-model="isAdmin"
                    />
                    Administrator-Rechte
                </label>
            </div>

            <Button
                class="register-button"
                @click="handleRegistration"
            >
                Registrieren
            </Button>

            <Button
                class="back-button"
                @click="goBack"
            >
                Zurück
            </Button>
        </div>
    </main>
</template>

<style scoped>
.registration-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #b2bec3 0%, #c3cfe2 100%);
    padding: 1.5rem;
}

.registration-box {
    background: white;
    padding: 2.5rem;
    border-radius: 16px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
    width: 100%;
    max-width: 400px;
}

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

.form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
}

.checkbox-group {
    flex-direction: row;
    align-items: center;
}

.checkbox-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-weight: 500;
    color: #2c3e50;
    cursor: pointer;
}

.form-group label {
    font-weight: 500;
    color: #2c3e50;
}

.form-control {
    padding: 0.75rem;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 1rem;
    transition: all 0.3s ease;
    background-color: #f8f9fa;
    color: #2c3e50;
}

.form-control:focus {
    outline: none;
    border-color: #409eff;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
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
    margin-bottom: 1rem;
}

:deep(.register-button:hover) {
    background-color: #219a52;
}

:deep(.register-button:disabled) {
    background-color: #a0aec0;
    cursor: not-allowed;
}

:deep(.back-button) {
    padding: 0.75rem 1.5rem;
    background-color: #e74c3c;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background-color 0.3s ease;
    width: 100%;
}

:deep(.back-button:hover) {
    background-color: #c0392b;
}
</style>
