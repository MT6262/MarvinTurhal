<template>
    <div class="assignee-form-container">
        <h1>{{ isEditing ? 'Edit Assignee' : 'Create Assignee' }}</h1>
        <form @submit.prevent="submitForm" class="assignee-form">
            <div class="form-group">
                <label for="prename">Prename:</label>
                <input type="text" class="form-control" id="prename" v-model="assignee.prename" required minlength="1" />
            </div>
            <div class="form-group">
                <label for="name">Name:</label>
                <input type="text" class="form-control" id="name" v-model="assignee.name" required minlength="1" />
            </div>
            <div class="form-group">
                <label for="email">Email:</label>
                <input type="email" class="form-control" id="email" v-model="assignee.email" required pattern="[a-z0-9._%+-]+@uni-stuttgart\.de$" />
                <small class="form-text text-muted">Email must end with @iste.uni-stuttgart.de</small>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">{{ isEditing ? 'Update' : 'Create' }} Assignee</button>
                <router-link to="/assignees" class="btn btn-danger ml-2">Cancel</router-link>
            </div>
        </form>
    </div>
</template>

<script>

import { assigneeFormLogic} from "@/components/Assignee/AssigneeForm.logic";

export default {
    name: 'CreateAssignee',
    ...assigneeFormLogic,
    mounted() {
        this.fetchAssignee();
    }
};

</script>

<style scoped>
.assignee-form-container {
    max-width: 600px;
    margin: 0 auto;
    padding: 20px;
    background-color: var(--color-white);
    border-radius: var(--border-radius);
    box-shadow: var(--box-shadow);
}

.assignee-form h1 {
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
</style>
