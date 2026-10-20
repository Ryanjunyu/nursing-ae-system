import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import EventReport from '../views/EventReport.vue'
import EventList from '../views/EventList.vue'
import RcaAnalysis from '../views/RcaAnalysis.vue'

const routes = [
  { path: '/login', name: 'login', component: Login },
  {
    path: '/',
    component: Layout,
    redirect: '/events',
    children: [
      { path: 'report', name: 'report', component: EventReport, meta: { requiresAuth: true } },
      { path: 'events', name: 'events', component: EventList, meta: { requiresAuth: true } },
      { path: 'rca', name: 'rca', component: RcaAnalysis, meta: { requiresAuth: true } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('ae_token')
  if (to.meta.requiresAuth && !token) return { name: 'login' }
  if (to.name === 'login' && token) return { name: 'events' }
})

export default router
