<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useRoute } from 'vue-router'
import { Close, Header, HeaderNav, HeaderNavItem, Toast, Toasts } from "agnostic-vue";
import { activeToasts } from "@/ts/toasts";
import {computed, onMounted, onUnmounted, ref, watch} from 'vue';

import 'agnostic-vue/dist/common.min.css';
import "agnostic-vue/dist/index.css";
import router from "@/router";
import {faPowerOff} from "@fortawesome/free-solid-svg-icons/faPowerOff";

const route = useRoute();
const showNavigation = computed(() => {
    return route.path !== '/' &&
        route.path !== '/login' &&
        route.path !== '/register' &&
        !route.path.includes('/workshop-feedback-login') &&
        !route.path.includes('/qr-feedback');
});

// Refs für reaktive Updates
const firstName = ref(localStorage.getItem('employeeFirstName'));
const lastName = ref(localStorage.getItem('employeeLastName'));
const isAdmin = ref(localStorage.getItem('isAdmin') === 'true');
const userType = computed(() => isAdmin.value ? 'Administrator' : 'Benutzer');

// Watches für Aktualisierungen
watch(() => localStorage.getItem('employeeFirstName'), (newVal) => {
    firstName.value = newVal;
});

watch(() => localStorage.getItem('employeeLastName'), (newVal) => {
    lastName.value = newVal;
});

watch(() => localStorage.getItem('isAdmin'), (newVal) => {
    isAdmin.value = newVal === 'true';
});

const updateUserInfo = () => {
    firstName.value = localStorage.getItem('employeeFirstName');
    lastName.value = localStorage.getItem('employeeLastName');
    isAdmin.value = localStorage.getItem('isAdmin') === 'true';
}

function handleLogout() {
    localStorage.clear();
    window.dispatchEvent(new Event('loginStateChanged'));
    router.replace('/');
}

onMounted(() => {
    window.addEventListener('loginStateChanged', updateUserInfo);
});

onUnmounted(() => {
    window.removeEventListener('loginStateChanged', updateUserInfo);
});
</script>

<template>
    <div id="app">
        <Header v-if="showNavigation" isHeaderContentStart>
            <template v-slot:headernav>
                <HeaderNav>
                    <HeaderNavItem>
                        <RouterLink v-if="!firstName" to="/">Login</RouterLink>
                        <a v-else @click="handleLogout" href="#" class="logout-link">
                            <font-awesome-icon :icon="faPowerOff" /> Abmelden
                        </a>
                    </HeaderNavItem>
                    <HeaderNavItem v-if="firstName">
                        <span>Angemeldet als {{userType}}: {{firstName}} {{lastName}}</span>
                    </HeaderNavItem>
                </HeaderNav>
            </template>
        </Header>

        <div class="main" :class="{ 'no-padding': !showNavigation }">
            <RouterView/>
        </div>
    </div>

    <Toasts vertical-position="top" horizontal-position="end">
        <template v-for="toast of activeToasts" :key="toast.key">
            <Toast :type="toast.type" class="alert alert-border-left alert-info">
                <div class="flex-fill flex flex-column">
                    <div class="flex">
                        <h3 class="flex-fill">{{ toast.title }}</h3>
                        <Close @click="toast.close()"/>
                    </div>
                    <div class="flex">
                        <font-awesome-icon :icon="toast.icon" size="xl" class="mie8 pbs2"></font-awesome-icon>
                        <div class="flex-fill">
                            {{ toast.message }}
                        </div>
                    </div>
                </div>
            </Toast>
            <div class="mbe14"/>
        </template>
    </Toasts>
</template>

<style scoped>
.main {
    padding: 10px 20px;
}

.main.no-padding {
    padding: 0;
}

.logout-link {
    cursor: pointer;
    color: #e74c3c;
    transition: color 0.3s ease;
}

.logout-link:hover {
    color: #c0392b;
}
</style>
