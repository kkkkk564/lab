import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/monitor',
    children: [
      { path: 'monitor', name: 'monitor', component: () => import('../views/MonitorView.vue'), meta: { title: '实时监控' } },
      { path: 'labs', name: 'labs', component: () => import('../views/LabsView.vue'), meta: { title: '实验室预约' } },
      { path: 'my-reservations', name: 'myReservations', component: () => import('../views/MyReservationsView.vue'), meta: { title: '我的预约' } },
      { path: 'approvals', name: 'approvals', component: () => import('../views/ApprovalsView.vue'), meta: { title: '预约审批', roles: ['ADMIN'] } },
      { path: 'devices', name: 'devices', component: () => import('../views/DevicesView.vue'), meta: { title: '设备管理' } },
      { path: 'lab-manage', name: 'labManage', component: () => import('../views/LabManageView.vue'), meta: { title: '实验室管理', roles: ['ADMIN'] } },
      { path: 'announcements', name: 'announcements', component: () => import('../views/AnnouncementsView.vue'), meta: { title: '公告通知' } },
      { path: 'users', name: 'users', component: () => import('../views/UsersView.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/monitor' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('lab_token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/monitor'
  }
  if (to.meta && to.meta.roles) {
    const user = JSON.parse(localStorage.getItem('lab_user') || 'null')
    if (!user || !to.meta.roles.includes(user.role)) {
      ElMessage.error('无权访问该页面')
      return '/monitor'
    }
  }
  document.title = to.meta && to.meta.title
    ? `${to.meta.title} · 校园实验室预约与监控系统`
    : '校园实验室预约与监控系统'
  return true
})

export default router
