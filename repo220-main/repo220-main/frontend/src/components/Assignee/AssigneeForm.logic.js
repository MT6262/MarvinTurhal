import axios from 'axios';
import { showToast, Toast } from "@/ts/toasts";

/**
 * Logic for assignee form component
 */
export const assigneeFormLogic = {
    data() {
        return {
            assignee: {
                id: '',
                prename: '',
                name: '',
                email: ''
            },
            originalEmail: '' // Speichert die ursprüngliche Email beim Laden
        };
    },

    computed: {
        isEditing() {
            return this.$route.name === 'EditAssignee';
        }
    },

    methods: {
        /**
         * Fetches the assignee details if the form is in edit mode
         * @returns {Promise<void>}
         */
        async fetchAssignee() {
            if (this.isEditing) {
                try {
                    const id = this.$route.params.id;
                    const response = await axios.get(`/api/v1/assignees/${id}`);
                    this.assignee = response.data;
                    this.originalEmail = response.data.email; // Speichere die originale Email
                } catch (error) {
                    console.error('Error fetching assignee:', error);
                    showToast(new Toast('Error', 'Failed to fetch assignee details', 'error'));
                    this.$router.push('/assignees');
                }
            }
        },

        /**
         * Submits the assignee form to the backend
         * @returns {Promise<void>}
         */
        async submitForm() {
            try {
                // Hole alle Assignees
                const response = await axios.get('/api/v1/assignees');

                // Prüfe nur auf doppelte Email wenn sich die Email geändert hat
                if (this.isEditing && this.assignee.email !== this.originalEmail) {
                    // Prüfe ob die neue Email bereits von einem anderen Assignee verwendet wird
                    const emailExists = response.data.some(a => a.email === this.assignee.email);
                    if (emailExists) {
                        showToast(new Toast('Error', 'This email is already associated with another assignee', 'error'));
                        return;
                    }
                } else if (!this.isEditing) {
                    // Bei neuem Assignee immer auf doppelte Email prüfen
                    const emailExists = response.data.some(a => a.email === this.assignee.email);
                    if (emailExists) {
                        showToast(new Toast('Error', 'This email is already associated with another assignee', 'error'));
                        return;
                    }
                }

                // Speichern oder Updaten
                if (this.isEditing) {
                    const id = this.$route.params.id;
                    await axios.put(`/api/v1/assignees/${id}`, this.assignee);
                    showToast(new Toast('Success', 'Assignee updated successfully', 'success'));
                } else {
                    await axios.post('/api/v1/assignees', this.assignee);
                    showToast(new Toast('Success', 'Assignee created successfully', 'success'));
                }
                this.$router.push('/assignees');
            } catch (error) {
                console.error('Error submitting form:', error);
                showToast(new Toast('Error', 'Failed to save assignee. Please check your input.', 'error'));
            }
        }
    }
};
