<template>
    <div class="home">
        <div class="welcome-container">
            <h1>Welcome to the ToDo App</h1>
            <p>Manage your tasks and assignees with ease!</p>
            <div class="quick-links">
                <RouterLink to="/todos" class="btn">View Todos</RouterLink>
                <RouterLink to="/assignees" class="btn">View Assignees</RouterLink>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">

</script>

<style scoped>
.home {
    display: flex;
    justify-content: center;
    align-items: center;
    height: calc(100vh - 100px); /* Adjust based on your header height */
    text-align: center;
}

.welcome-container {
    max-width: 600px;
    padding: 2rem;
    background-color: #f4f4f4;
    border-radius: 10px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

h1 {
    color: #333;
    margin-bottom: 1rem;
}

p {
    color: #666;
    margin-bottom: 2rem;
}

.quick-links {
    display: flex;
    justify-content: center;
    gap: 1rem;
}

.btn {
    display: inline-block;
    padding: 0.75rem 1.5rem;
    background-color: #007bff;
    color: white;
    text-decoration: none;
    border-radius: 5px;
    transition: background-color 0.3s ease;
}

.btn:hover {
    background-color: #0056b3;
}
</style>
