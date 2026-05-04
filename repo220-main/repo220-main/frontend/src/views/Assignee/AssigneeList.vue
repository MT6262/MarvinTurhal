<template>
    <div class="assignee-list-container">
        <h1>Assignees</h1>

        <div class="actions mb-3">
            <router-link to="/create-assignee" class="btn btn-primary">
                Create New Assignee
            </router-link>
        </div>

        <table class="table table-striped">
            <thead>
            <tr>
                <th>ID</th>
                <th>Prename</th>
                <th>Name</th>
                <th>Email</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="assignee in assignees" :key="assignee.id">
                <td>{{ assignee.id }}</td>
                <td>{{ assignee.prename }}</td>
                <td>{{ assignee.name }}</td>
                <td>{{ assignee.email }}</td>
                <td>
                    <div class="btn-group">
                        <router-link :to="`/assignees/${assignee.id}`" class="btn btn-sm btn-primary ml-2">
                            Edit
                        </router-link>
                        <button
                            @click="deleteAssignee(assignee.id)"
                            class="btn btn-sm btn-danger"
                        >
                            Delete
                        </button>
                    </div>
                </td>
            </tr>
            </tbody>
        </table>

        <p v-if="assignees.length === 0" class="text-center">
            No assignees found.
        </p>
    </div>
</template>

<script>

import { assigneeListLogic} from "@/components/Assignee/AssigneeList.logic";

export default {
    name: 'AssigneeList',
    ...assigneeListLogic,
    mounted() {
        this.fetchAssignees();
    }
};

</script>
