<template>
  <div class="verify-container">
    <div class="verify-card">
      <div v-if="isLoading" class="loading-state">
        <h1 class="verify-title">驗證中...</h1>
        <p class="verify-message">正在驗證您的電子郵件地址，請稍候...</p>
      </div>

      <div v-else-if="success" class="success-state">
        <h1 class="verify-title">✓ 驗證成功</h1>
        <p class="verify-message">{{ successMessage }}</p>
        <router-link to="/login" class="btn btn-primary">前往登入</router-link>
      </div>

      <div v-else-if="error" class="error-state">
        <h1 class="verify-title">✗ 驗證失敗</h1>
        <p class="verify-message">{{ error }}</p>
        <div class="actions">
          <router-link to="/login" class="btn btn-secondary">前往登入</router-link>
          <router-link to="/register" class="btn btn-primary">重新註冊</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isLoading = ref(true)
const success = ref(false)
const error = ref('')
const successMessage = ref('您的電子郵件已成功驗證，現在可以登入系統了。')

const verifyEmail = async (token) => {
  isLoading.value = true
  error.value = ''
  success.value = false

  try {
    await authStore.verifyEmail(token)
    success.value = true
    successMessage.value = '您的電子郵件已成功驗證，現在可以登入系統了。'
  } catch (err) {
    if (err.response?.status === 400) {
      error.value = err.response.data || '驗證連結無效或已過期'
    } else {
      error.value = '驗證失敗，請稍後再試或重新申請註冊'
    }
    console.error('Email verification error:', err)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  const token = route.query.token

  if (!token) {
    error.value = '缺少驗證 Token，請檢查您的驗證連結是否完整'
    isLoading.value = false
    return
  }

  verifyEmail(token)
})
</script>

<style scoped>
.verify-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 1rem;
}

.verify-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  padding: 3rem;
  width: 100%;
  max-width: 500px;
  text-align: center;
}

.verify-title {
  color: #333;
  font-size: 2rem;
  margin-bottom: 1.5rem;
}

.verify-message {
  color: #666;
  font-size: 1.1rem;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.loading-state {
  padding: 1rem;
}

.success-state .verify-title {
  color: #28a745;
}

.error-state .verify-title {
  color: #e74c3c;
}

.actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.btn {
  padding: 0.75rem 2rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  display: inline-block;
}

.btn-primary {
  background-color: #667eea;
  color: white;
}

.btn-primary:hover {
  background-color: #5568d3;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

@media (max-width: 480px) {
  .verify-card {
    padding: 2rem 1.5rem;
  }

  .verify-title {
    font-size: 1.5rem;
  }

  .verify-message {
    font-size: 1rem;
  }

  .actions {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }
}
</style>

