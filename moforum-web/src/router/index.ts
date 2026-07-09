import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'b/:boardId',
          name: 'board',
          component: () => import('@/views/BoardView.vue'),
        },
        {
          path: 'b/:boardId/new',
          name: 'post-new',
          component: () => import('@/views/PostCreateView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'p/:id',
          name: 'post-detail',
          component: () => import('@/views/PostDetailView.vue'),
        },
        {
          path: 'search',
          name: 'search',
          component: () => import('@/views/SearchView.vue'),
        },
        {
          path: 'u/:userId',
          name: 'user-profile',
          component: () => import('@/views/UserProfileView.vue'),
        },
        {
          path: 'friends',
          name: 'friends',
          component: () => import('@/views/FriendsView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'chat',
          name: 'chat',
          component: () => import('@/views/ChatView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'chat/:friendId',
          name: 'chat-with',
          component: () => import('@/views/ChatView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'user/query',
          name: 'user-query',
          component: () => import('@/views/UserQueryView.vue'),
        },
        {
          path: 'admin',
          name: 'admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
    {
      path: '/auth',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        {
          path: 'login',
          name: 'login',
          component: () => import('@/views/LoginView.vue'),
        },
        {
          path: 'register',
          name: 'register',
          component: () => import('@/views/RegisterView.vue'),
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.requiresAuth) return true
  const user = useUserStore()
  if (user.isLoggedIn) return true
  return { name: 'login', query: { redirect: to.fullPath } }
})

export default router
