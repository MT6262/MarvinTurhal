import { createRouter, createWebHistory } from 'vue-router'
import ToDoList from '../views/Todo/ToDoList.vue'
import CreateToDo from '../views/Todo/CreateToDo.vue'
import EditToDo from '../views/Todo/EditToDo.vue'
import AssigneeList from '../views/Assignee/AssigneeList.vue'
import CreateAssignee from '../views/Assignee/CreateAssignee.vue'
import EditAssignee from '../views/Assignee/EditAssignee.vue'



const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
      {
      path: '/',
      name: 'Home',
      component: () => import('../views/HomeView.vue')
      },
      {
          path: '/assignees',
          name: 'AssigneeList',
          component: AssigneeList
      },
      {
          path: '/create-assignee',
          name: 'CreateAssignee',
          component: CreateAssignee
      },
      {
          path: '/assignees/:id',
          name: 'EditAssignee',
          component: EditAssignee
      },
      {
      path: '/todos',
    name: 'ToDoList',
    component: ToDoList
},
{
    path: '/create-todo',
        name: 'CreateToDo',
    component: CreateToDo
},
{
    path: '/todos/:id',
        name: 'EditToDo',
    component: EditToDo
}
  ]
})

export default router
