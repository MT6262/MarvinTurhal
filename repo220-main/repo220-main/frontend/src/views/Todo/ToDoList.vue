<template>
    <div class="todo-list-container">
        <h1>ToDos</h1>

        <div class="todo-controls mb-3">
            <div class="row">
                <div class="col-md-4">
                    <router-link to="/create-todo" class="btn btn-primary">
                        Create New ToDo
                    </router-link>
                </div>
                <div class="col-md-4">
                    <button
                        @click="exportToCsv"
                        class="btn btn-secondary"
                    >
                        Export To CSV
                    </button>
                </div>
                <div class="col-md-8">
                    <div class="input-group">
                        <input
                            type="text"
                            class="form-control"
                            placeholder="Filter by title"
                            v-model="titleFilter"
                        >
                        <select
                            class="form-control"
                            v-model="sortField"
                        >
                            <option value="title">Sort by Title</option>
                            <option value="dueDate">Sort by Due Date</option>
                            <option value="createdDate">Sort by Created Date</option>
                        </select>
                        <select
                            class="form-control"
                            v-model="sortDirection"
                        >
                            <option value="asc">Ascending</option>
                            <option value="desc">Descending</option>
                        </select>
                    </div>
                </div>
            </div>
        </div>

        <div class="todo-section">
            <h2>Open Tasks</h2>
            <table class="table table-striped">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Description</th>
                    <th>Category</th> <!-- New Category Column -->
                    <th>Assignees</th>
                    <th>Created Date</th>
                    <th>Due Date</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="todo in openTodos" :key="todo.id">
                    <td>{{ todo.id }}</td>
                    <td>{{ todo.title }}</td>
                    <td>{{ todo.description }}</td>
                    <td>{{ todo.category }}</td> <!-- Display Category -->
                    <td>
                        <span
                            v-for="(assignee, index) in todo.assigneeList"
                            :key="assignee.id"
                            @click="showAssigneeDetails(assignee)"
                            class="clickable"
                        >
                            {{ assignee.prename }} {{ assignee.name }}
                            {{ index < todo.assigneeList.length - 1 ? ', ' : '' }}
                        </span>
                        <span v-if="todo.assigneeList.length === 0">Unassigned</span>
                    </td>
                    <td>{{ formatDate(todo.createdDate) }}</td>
                    <td>{{ formatDate(todo.dueDate) }}</td>
                    <td>
                        <button
                            @click="markAsDone(todo)"
                            class="btn btn-sm btn-secondary ml-2"
                        >
                            Done
                        </button>
                        <router-link
                            :to="{ name: 'EditToDo', params: { id: todo.id } }"
                            class="btn btn-sm btn-primary ml-2"
                        >
                            Edit
                        </router-link>
                        <button
                            @click="deleteTodo(todo.id)"
                            class="btn btn-sm btn-danger ml-2"
                        >
                            Delete
                        </button>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>

        <div class="todo-section">
            <h2>
                Completed Tasks
                <span class="clickable" @click="toggleCompletedTodos">
                    <span class="badge">{{ completedTodos.length }}</span>
                    <i class="ml-2">{{ showCompletedTodos ? '▼' : '►' }}</i>
                </span>
            </h2>

            <table v-if="showCompletedTodos" class="table table-striped">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Description</th>
                    <th>Category</th> <!-- New Category Column -->
                    <th>Assignees</th>
                    <th>Created Date</th>
                    <th>Finished Date</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="todo in completedTodos" :key="todo.id">
                    <td>{{ todo.id }}</td>
                    <td>{{ todo.title }}</td>
                    <td>{{ todo.description }}</td>
                    <td>{{ todo.category }}</td> <!-- Display Category -->
                    <td>
                        <span
                            v-for="(assignee, index) in todo.assigneeList"
                            :key="assignee.id"
                            @click="showAssigneeDetails(assignee)"
                            class="clickable"
                        >
                            {{ assignee.prename }} {{ assignee.name }}
                            {{ index < todo.assigneeList.length - 1 ? ', ' : '' }}
                        </span>
                        <span v-if="todo.assigneeList.length === 0">Unassigned</span>
                    </td>
                    <td>{{ formatDate(todo.createdDate) }}</td>
                    <td>{{ formatDate(todo.finishedDate) }}</td>
                    <td>
                        <button
                            @click="deleteTodo(todo.id)"
                            class="btn btn-sm btn-danger"
                        >
                            Delete
                        </button>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>

        <!-- Modal for displaying assignee details -->
        <div v-if="showAssigneeModal" class="modal-overlay" @click="closeAssigneeModal">
            <div class="modal-content" @click.stop>
                <h2>Assignee Details</h2>
                <p><strong>ID:</strong> {{ selectedAssignee.id }}</p>
                <p><strong>Prename:</strong> {{ selectedAssignee.prename }}</p>
                <p><strong>Name:</strong> {{ selectedAssignee.name }}</p>
                <p><strong>Email:</strong> {{ selectedAssignee.email }}</p>
                <button @click="closeAssigneeModal" class="btn btn-danger">Close</button>
            </div>
        </div>
    </div>
</template>

<script>
import { todoLogic} from "@/components/todo/todoList.logic";

export default {
    name: 'ToDoList',
    ...todoLogic,
    mounted() {
        this.fetchTodos();
    }
};
</script>

<style scoped>
.clickable {
    cursor: pointer;
}

/* Modal styles */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
}

.modal-content {
    background: white;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
    max-width: 500px;
    width: 100%;
}

.modal-content h2 {
    margin-top: 0;
}

.modal-content p {
    margin: 10px 0;
}

.modal-content .btn {
    margin-top: 20px;
}
</style>
