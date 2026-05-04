import axios from 'axios';
import { showToast, Toast } from "@/ts/toasts";


/**
 * Logic for assignee list component
 */
export const assigneeListLogic = {
    data() {
        return {
            assignees: []
        };
    },

    methods: {
        /**
         * Fetches all assignees from the backend
         * @returns {Promise<void>}
         */
        async fetchAssignees() {
            try {
                const response = await axios.get('/api/v1/assignees');
                this.assignees = response.data;
            } catch (error) {
                console.error('Error fetching assignees:', error);
                showToast(new Toast('Error', 'Failed to fetch assignees', 'error'));
            }
        },
        /**
         * Deletes an assignee from the backend
         * @param id - The ID of the assignee to delete
         * @returns {Promise<void>}
         */
        async deleteAssignee(id) {
            if (confirm('Are you sure you want to delete this assignee?')) {
                try {
                    await axios.delete(`/api/v1/assignees/${id}`);
                    this.assignees = this.assignees.filter(assignee => assignee.id !== id);
                    showToast(new Toast('Success', 'Assignee deleted successfully', 'success'));
                } catch (error) {
                    console.error('Error deleting assignee:', error);
                    showToast(new Toast('Error', 'Failed to delete assignee', 'error'));
                }
            }
        }
    }
};
