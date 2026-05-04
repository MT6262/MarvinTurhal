<template>
    <div class="todo-form-container">
        <h1>Edit ToDo</h1>
        <form @submit.prevent="updateToDo" class="todo-form">
            <div class="form-group">
                <label for="title">Title:</label>
                <input type="text" class="form-control" id="title" v-model="todo.title" required />
            </div>
            <div class="form-group">
                <label for="description">Description:</label>
                <input type="text" class="form-control" id="description" v-model="todo.description" />
            </div>
            <div class="form-group">
                <label for="dueDate">Due Date:</label>
                <input type="date" class="form-control" id="dueDate" v-model="todo.dueDate" />
            </div>
            <div class="form-group">
                <label for="category">Category:</label>
                <input type="text" class="form-control" id="category" v-model="todo.category" readonly />
            </div>
            <div class="form-group">
                <label for="assignees">[ID]Assignees:</label>
                <select
                    class="form-control"
                    id="assignees"
                    v-model="todo.assigneeIds"
                    multiple
                >
                    <option v-for="assignee in assignees" :key="assignee.id" :value="assignee.id">
                       [{{ assignee.id }}] {{ assignee.prename }} {{ assignee.name }}
                    </option>
                </select>
                <small class="form-text text-muted">
                    Hold Ctrl (or Cmd on Mac) to select multiple assignees , or to deselect assignees
                </small>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">Update</button>
                <router-link to="/todos" class="btn btn-danger ml-2">Cancel</router-link>
            </div>
        </form>
    </div>
</template>

<script>

import { todoFormLogic} from "@/components/todo/todoForm.logic";

export default {
    name: 'EditToDo',
    ...todoFormLogic,
    created() {
        this.fetchToDo();
        this.fetchAssignees();
    }
};

</script>

<style scoped>
.todo-form-container {
    max-width: 600px;
    margin: 0 auto;
    padding: 20px;
    background-color: var(--color-white);
    border-radius: var(--border-radius);
    box-shadow: var(--box-shadow);
}

.todo-form h1 {
    color: var(--color-primary);
    text-align: center;
    margin-bottom: 30px;
    font-weight: 700;
}

.form-group {
    margin-bottom: 15px;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

select[multiple] {
    height: 150px;
}
</style>
