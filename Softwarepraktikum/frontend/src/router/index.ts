import { createRouter, createWebHashHistory  } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import AdminView from "@/views/admin/AdminView.vue";
import CreateWorkshopView from "@/views/admin/CreateWorkshopView.vue";
import CreateExchangeDayView from "@/views/admin/CreateExchangeDayView.vue";
import EmployeeCalendarView from "@/views/employee/EmployeeCalendarView.vue";
import RegistrationEmployeeView from "@/views/RegistrationEmployeeView.vue";
import FeedbackView from "@/views/employee/FeedbackView.vue";
import FeedbackDashboardView from "@/views/admin/FeedbackDashboardView.vue";
import EmployeeDashboardView from "@/views/employee/EmployeeDashboardView.vue";
import EmployeeFeedbackOverview from "@/views/employee/EmployeeFeedbackOverview.vue";

const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView
        },
        {
            path: '/admin',
            name: 'admin',
            component: AdminView
        },
        {
            path: '/user',
            name: 'user',
            component: EmployeeCalendarView
        },
        {
            path: '/workshop/create',
            name: 'create-workshop',
            component: CreateWorkshopView
        },
        {
            path: '/exchangeday/create',
            name: 'create-exchangeday',
            component: CreateExchangeDayView
        },
        {
            path: '/register',
            name: 'register',
            component: RegistrationEmployeeView
        },
        {
            path: '/feedback/:workshopId',
            name: 'feedback',
            component: FeedbackView
        },
        {
            path: '/admin/feedback',
            name: 'feedback-dashboard',
            component: FeedbackDashboardView
        },
        {
            path: '/my-feedback',
            name: 'employee-feedback',
            component: EmployeeFeedbackOverview
        },
        {
            path: '/workshop/:id',
            name: 'WorkshopDetails',
            component: () => import('@/views/admin/WorkshopDetails.vue')
        },
        {
            path: '/workshop/:id/edit',
            name: 'WorkshopEdit',
            component: () => import('@/views/admin/WorkshopEdit.vue')
        },
        {
            path: '/employee-dashboard',
            name: 'EmployeeDashboard',
            component: () => import('@/views/employee/EmployeeDashboardView.vue')
        },
        {
            path: '/exchangeday/:id/edit',
            name: 'ExchangeDayEdit',
            component: () => import('@/views/admin/ExchangeDayEditView.vue')
        },
        {
            path: '/workshop-feedback-login/:workshopId',
            name: 'workshop-feedback-login',
            component: () => import('@/views/WorkshopFeedbackLogin.vue')
        },
        {
            path: '/qr-feedback/:workshopId',
            name: 'qr-feedback',
            component: () => import('@/views/QRFeedbackView.vue')
        }
    ]
})

function validateSession() {
    const sessionStartTime = localStorage.getItem('sessionStartTime');
    if (!sessionStartTime) return false;

    // Session nach 24h ungültig
    const now = new Date().getTime();
    const sessionAge = now - parseInt(sessionStartTime);
    return sessionAge < 24 * 60 * 60 * 1000;
}



router.beforeEach((to, from, next) => {
    if (to.name === 'workshop-feedback-login' || to.name === 'qr-feedback') {
        next();
        return;
    }

    const isLoggedIn = localStorage.getItem('employeeId') && validateSession();

    if (to.path === '/' || to.path === '/register') {
        next();
        return;
    }

    if (!isLoggedIn) {
        localStorage.clear();
        next('/');
        return;
    }

    next();
});
export default router
