<template>
  <nav class="navbar">
    <div class="navbar-container">
      <!-- Logo / 應用程式標題 -->
      <div class="navbar-brand">
        <router-link to="/records" class="brand-link">
          <span class="brand-icon">📊</span>
          <span class="brand-text">Health Log</span>
        </router-link>
      </div>

      <!-- 導航連結 -->
      <div class="navbar-links">
        <router-link
          to="/records"
          class="nav-link"
          active-class="active"
        >
          <span class="nav-icon">📝</span>
          <span class="nav-text">記錄</span>
        </router-link>
        <router-link
          to="/reports"
          class="nav-link"
          active-class="active"
        >
          <span class="nav-icon">📈</span>
          <span class="nav-text">報告</span>
        </router-link>
        <router-link
          v-if="isAdmin"
          to="/admin/settings"
          class="nav-link"
          active-class="active"
        >
          <span class="nav-icon">⚙️</span>
          <span class="nav-text">設定</span>
        </router-link>
      </div>

      <!-- 用戶資訊與登出 -->
      <div class="navbar-user">
        <div class="user-info">
          <span class="user-email">{{ userEmail }}</span>
        </div>
        <button
          @click="handleLogout"
          class="btn-logout"
          title="登出"
        >
          <span class="logout-icon">🚪</span>
          <span class="logout-text">登出</span>
        </button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

// 計算屬性
const userEmail = computed(() => authStore.user?.email || '')
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

// 登出處理
const handleLogout = async () => {
  try {
    await authStore.logout()
    router.push('/login')
  } catch (error) {
    console.error('Logout error:', error)
    // 即使登出 API 失敗，也強制跳轉到登入頁
    router.push('/login')
  }
}
</script>

<style scoped>
.navbar {
  background-color: #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

/* Logo / Brand */
.navbar-brand {
  display: flex;
  align-items: center;
}

.brand-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #333;
  font-size: 1.25rem;
  font-weight: 600;
  transition: color 0.2s;
}

.brand-link:hover {
  color: #4a90e2;
}

.brand-icon {
  font-size: 1.5rem;
  margin-right: 0.5rem;
}

.brand-text {
  font-weight: 600;
}

/* 導航連結 */
.navbar-links {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  justify-content: center;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  text-decoration: none;
  color: #666;
  border-radius: 6px;
  transition: all 0.2s;
  font-size: 0.95rem;
}

.nav-link:hover {
  background-color: #f5f5f5;
  color: #333;
}

.nav-link.active {
  background-color: #4a90e2;
  color: white;
}

.nav-icon {
  font-size: 1.1rem;
}

.nav-text {
  font-weight: 500;
}

/* 用戶資訊與登出 */
.navbar-user {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-email {
  color: #666;
  font-size: 0.9rem;
}

.btn-logout {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background-color: transparent;
  border: 1px solid #ddd;
  border-radius: 6px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.btn-logout:hover {
  background-color: #f5f5f5;
  border-color: #bbb;
  color: #333;
}

.logout-icon {
  font-size: 1rem;
}

/* 響應式設計 */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 1rem;
    height: 56px;
  }

  /* 手機版：隱藏圖示，顯示文字 */
  .brand-icon,
  .nav-icon,
  .logout-icon {
    display: none;
  }

  .brand-text,
  .nav-text,
  .logout-text {
    display: inline;
  }

  .nav-link {
    padding: 0.5rem;
  }

  .btn-logout {
    padding: 0.5rem;
  }

  .user-email {
    display: none;
  }
}

@media (max-width: 480px) {
  .navbar-links {
    gap: 0.25rem;
  }

  .nav-link {
    padding: 0.4rem;
  }
}
</style>

