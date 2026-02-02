import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Auth/Login.vue'),
      meta: {
        requiresAuth: false, // 登入頁面不需要認證
      },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Auth/Register.vue'),
      meta: {
        requiresAuth: false, // 註冊頁面不需要認證
      },
    },
    {
      path: '/verify-email',
      name: 'VerifyEmail',
      component: () => import('@/views/Auth/VerifyEmail.vue'),
      meta: {
        requiresAuth: false, // 驗證頁面不需要認證
      },
    },
    {
      path: '/records',
      name: 'RecordList',
      component: () => import('@/views/Records/RecordList.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/records/new',
      name: 'RecordNew',
      component: () => import('@/views/Records/RecordForm.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/records/:date',
      name: 'RecordEdit',
      component: () => import('@/views/Records/RecordForm.vue'),
      meta: {
        requiresAuth: true,
      },
      props: true, // 將路由參數作為 props 傳遞
    },
    {
      path: '/reports',
      name: 'TrendChart',
      component: () => import('@/views/Reports/TrendChart.vue'),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/admin/settings',
      name: 'FieldSettings',
      component: () => import('@/views/Admin/FieldSettings.vue'),
      meta: {
        requiresAuth: true,
        requiresAdmin: true, // 需要 Admin 權限
      },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/', // 404 重定向到首頁
    },
  ],
})

// 路由守衛
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 檢查是否需要認證
  if (to.meta.requiresAuth) {
    // 如果未登入，導向登入頁
    if (!authStore.isAuthenticated) {
      next({
        name: 'Login',
        query: { redirect: to.fullPath }, // 保存原始路徑，登入後可以重定向回來
      })
      return
    }

    // 檢查是否需要 Admin 權限
    if (to.meta.requiresAdmin) {
      const user = authStore.user
      // 檢查用戶角色是否為 ADMIN
      if (!user || user.role !== 'ADMIN') {
        // 沒有 Admin 權限，導向首頁並顯示錯誤訊息
        next({
          name: 'Home',
          query: { error: 'unauthorized' },
        })
        return
      }
    }
  }

  // 如果已登入且訪問登入/註冊/驗證頁，重定向到首頁
  if ((to.name === 'Login' || to.name === 'Register' || to.name === 'VerifyEmail') && authStore.isAuthenticated) {
    // 檢查是否有重定向路徑
    const redirect = from.query.redirect || '/'
    next(redirect)
    return
  }

  // 允許訪問
  next()
})

export default router
