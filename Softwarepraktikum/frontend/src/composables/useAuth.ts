// useAuth.ts
import { ref, computed } from 'vue'

export const useAuth = () => {
    const mitarbeiterId = ref(localStorage.getItem('employeeId'))
    const mitarbeiterEmail = ref(localStorage.getItem('employeeEmail'))
    const istAdmin = ref(localStorage.getItem('isAdmin') === 'true')

    const istAngemeldet = computed(() => !!mitarbeiterId.value)
    const benutzerTyp = computed(() => istAdmin.value ? 'Administrator' : 'Benutzer')

    const abmelden = () => {
        localStorage.removeItem('employeeId')
        localStorage.removeItem('employeeEmail')
        localStorage.removeItem('isAdmin')
        mitarbeiterId.value = null
        mitarbeiterEmail.value = null
        istAdmin.value = false
    }

    return {
        mitarbeiterId,
        mitarbeiterEmail,
        istAdmin,
        istAngemeldet,
        benutzerTyp,
        abmelden
    }
}
