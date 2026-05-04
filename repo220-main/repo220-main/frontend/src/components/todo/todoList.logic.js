
import axios from 'axios';
import { saveAs } from 'file-saver';
import { showToast, Toast } from "@/ts/toasts";

/**
 * Logic for todo list component
 */
export const todoLogic = {
    data() {
        return {
            todos: [],
            titleFilter: '',
            sortField: 'title',
            sortDirection: 'asc',
            showCompletedTodos: false,
            showAssigneeModal: false,
            selectedAssignee: {}
        };
    },

    computed: {
        /**
         * Filters and sorts the todos based on the title filter, sort field and sort direction
         * @returns {*[]}
         */
        filteredTodos() {
            let filtered = this.todos.filter(todo =>
                todo.title.toLowerCase().includes(this.titleFilter.toLowerCase())
            );

            return filtered.sort((a, b) => {
                let modifier = this.sortDirection === 'asc' ? 1 : -1;

                if (this.sortField === 'title') {
                    return modifier * a.title.localeCompare(b.title);
                } else if (this.sortField === 'dueDate') {
                    return modifier * (new Date(a.dueDate) - new Date(b.dueDate));
                } else if (this.sortField === 'createdDate') {
                    return modifier * (new Date(a.createdDate) - new Date(b.createdDate));
                }
                return 0;
            });
        },

        openTodos() {
            return this.filteredTodos.filter(todo => !todo.finished);
        },

        completedTodos() {
            return this.filteredTodos.filter(todo => todo.finished);
        }
    },

    methods: {
        /**
         * Fetches all todos from the backend
         * @returns {Promise<void>}
         */
        async fetchTodos() {
            try {
                const response = await axios.get('/api/v1/todos');
                this.todos = response.data;
            } catch (error) {
                console.error('Error fetching todos:', error);
                showToast(new Toast('Error', 'Failed to fetch todos', 'error'));
            }
        },
        /**
         * Marks a todo as done
         * @param todo - The todo to mark as done
         * @returns {Promise<void>}
         */
        async markAsDone(todo) {
            try {
                const todoToUpdate = {
                    ...todo,
                    finished: true,
                    assigneeIdList: todo.assigneeList.map(assignee => assignee.id)
                };
                await axios.put(`/api/v1/todos/${todo.id}`, todoToUpdate);
                await this.fetchTodos();
                showToast(new Toast('Success', 'Todo marked as done', 'success'));
            } catch (error) {
                console.error('Error marking todo as done:', error);
                showToast(new Toast('Error', 'Failed to mark todo as done', 'error'));
            }
        },
        /**
         * Deletes a todo from the backend
         * @param id - The ID of the todo to delete
         * @returns {Promise<void>}
         */
        async deleteTodo(id) {
            if (confirm('Are you sure you want to delete this todo?')) {
                try {
                    await axios.delete(`/api/v1/todos/${id}`);
                    await this.fetchTodos();
                    showToast(new Toast('Success', 'Todo deleted successfully', 'success'));
                } catch (error) {
                    console.error('Error deleting todo:', error);
                    showToast(new Toast('Error', 'Failed to delete todo', 'error'));
                }
            }
        },
        /**
         * Exports all todos to a CSV file
         * @returns {Promise<void>}
         */
        async exportToCsv() {
            try {
                const response = await axios.get('/api/v1/csv-downloads/todos', {
                    responseType: 'blob'
                });
                const blob = new Blob([response.data], {
                    type: 'text/csv;charset=utf-8'
                });
                saveAs(blob, 'todos.csv');
                showToast(new Toast('Success', 'CSV exported successfully', 'success'));
            } catch (error) {
                console.error('Error exporting CSV:', error);
                showToast(new Toast('Error', 'Failed to export todos', 'error'));
            }
        },
        /**
         * Formats a timestamp to a readable date string
         * @param timestamp - The timestamp to format
         * @returns {string}
         */
        formatDate(timestamp) {
            if (!timestamp) return 'No date';
            return new Date(timestamp).toLocaleDateString();
        },
        /**
         * Shows the assignee details modal
         * @param assignee - The assignee to show details for
         */
        showAssigneeDetails(assignee) {
            this.selectedAssignee = assignee;
            this.showAssigneeModal = true;
        },
        closeAssigneeModal() {
            this.showAssigneeModal = false;
        },
        toggleCompletedTodos() {
            this.showCompletedTodos = !this.showCompletedTodos;
        }
    }
};
