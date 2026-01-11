import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { useAuthStore } from './stores/authStore'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())

// 初始化認證狀態（使用 refresh token 獲取新的 access token）
// 注意：即使失敗也要掛載應用程式，讓使用者可以登入
const initApp = async () => {
  const authStore = useAuthStore()
  
  // 先初始化認證狀態
  try {
    await authStore.init()
  } catch (error) {
    // 初始化失敗表示未登入或 session 已過期，這是正常的
    console.log('No valid session found, user needs to login')
  } finally {
    app.use(router)
    app.mount('#app')
  }
}

initApp()