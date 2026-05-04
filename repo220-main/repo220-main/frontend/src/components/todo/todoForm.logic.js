import axios from 'axios';
import { showToast, Toast } from "@/ts/toasts";

/**
 * Logic for todo form component
 */
export const todoFormLogic = {
    data() {
        return {
            todo: {
                title: '',
                description: '',
                dueDate: '',
                assigneeIds: []
            },
            assignees: []
        };
    },

    methods: {
        /**
         * Formats a date string to YYYY-MM-DD format for the date input
         * @param {string} dateString - The date string to format
         * @returns {string} - Formatted date string
         */
        formatDateForInput(dateString) {
            if (!dateString) return '';
            const date = new Date(dateString);
            return date.toISOString().split('T')[0];
        },

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
         * Fetches the todo details if the form is in edit mode
         * @returns {Promise<void>}
         */
        async fetchToDo() {
            try {
                const id = this.$route.params.id;
                const response = await axios.get(`/api/v1/todos/${id}`);
                const todoData = response.data;

                this.todo = {
                    ...todoData,
                    dueDate: this.formatDateForInput(todoData.dueDate),
                    assigneeIds: todoData.assigneeList.map(assignee => assignee.id)
                };
            } catch (error) {
                console.error('Error fetching todo:', error);
                showToast(new Toast('Error', 'Failed to fetch todo', 'error'));
            }
        },

        /**
         * Submits the todo form to the backend
         * @returns {Promise<void>}
         */
        async updateToDo() {
            try {
                const id = this.$route.params.id;
                const todoToSubmit = {
                    ...this.todo,
                    assigneeIdList: this.todo.assigneeIds
                };

                await axios.put(`/api/v1/todos/${id}`, todoToSubmit);
                showToast(new Toast('Success', 'ToDo updated successfully', 'success'));
                this.$router.push('/todos');
            } catch (error) {
                console.error('Error updating todo:', error);
                showToast(new Toast('Error', 'Failed to update todo', 'error'));
            }
        },

        /**
         * Submits the todo form to the backend
         * @returns {Promise<void>}
         */
        async createToDo() {
            try {
                const todoToSubmit = {
                    ...this.todo,
                    assigneeIdList: this.todo.assigneeIds
                };

                await axios.post('/api/v1/todos', todoToSubmit);
                showToast(new Toast('Success', 'ToDo created successfully', 'success'));
                this.$router.push('/todos');
            } catch (error) {
                console.error('Error creating todo:', error);
                showToast(new Toast('Error', 'Failed to create todo', 'error'));
            }
        }
    }
};
