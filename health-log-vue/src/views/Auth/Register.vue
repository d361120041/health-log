<template>
  <div class="register-container">
    <div class="register-card">
      <h1 class="register-title">註冊帳號</h1>
      <form @submit.prevent="handleRegister" class="register-form">
        <div v-if="error" class="error-message">{{ error }}</div>
        <div v-if="success" class="success-message">{{ success }}</div>
        
        <div class="form-group">
          <label for="email">電子郵件</label>
          <input
            id="email"
            v-model="email"
            type="email"
            required
            placeholder="請輸入電子郵件"
            class="form-input"
            :disabled="isLoading"
          />
        </div>

        <div class="form-group">
          <label for="password">密碼</label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            placeholder="請輸入密碼（至少 8 個字元）"
            class="form-input"
            :disabled="isLoading"
            minlength="8"
          />
        </div>

        <div class="form-group">
          <label for="confirmPassword">確認密碼</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            required
            placeholder="請再次輸入密碼"
            class="form-input"
            :disabled="isLoading"
            minlength="8"
          />
        </div>

        <button
          type="submit"
          class="btn btn-primary"
          :disabled="isLoading"
        >
          {{ isLoading ? '註冊中...' : '註冊' }}
        </button>

        <div class="login-link">
          <p>已經有帳號？<router-link to="/login">前往登入</router-link></p>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const isLoading = ref(false)
const error = ref('')
const success = ref('')

const handleRegister = async () => {
  error.value = ''
  success.value = ''

  // 前端驗證：確認密碼是否一致
  if (password.value !== confirmPassword.value) {
    error.value = '密碼與確認密碼不一致'
    return
  }

  // 前端驗證：密碼長度
  if (password.value.length < 8) {
    error.value = '密碼長度至少需要 8 個字元'
    return
  }

  isLoading.value = true

  try {
    await authStore.register({
      email: email.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
    })
    
    // 註冊成功，顯示成功訊息
    success.value = '註冊成功！請檢查您的電子郵件以驗證帳號。'
    
    // 3 秒後重定向到登入頁
    setTimeout(() => {
      router.push('/login')
    }, 3000)
  } catch (err) {
    if (err.response?.status === 400) {
      error.value = err.response.data || '此電子郵件已被註冊或註冊資料格式錯誤'
    } else {
      error.value = '註冊失敗，請稍後再試'
    }
    console.error('Register error:', err)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  // 如果已經登入，重定向到記錄列表
  if (authStore.isAuthenticated) {
    router.back()
  }
})
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 1rem;
}

.register-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  padding: 2.5rem;
  width: 100%;
  max-width: 400px;
}

.register-title {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
  font-size: 1.75rem;
}

.register-form {
  width: 100%;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #333;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.form-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.error-message {
  background-color: #fee;
  color: #e74c3c;
  padding: 0.75rem;
  border-radius: 4px;
  margin-bottom: 1rem;
  border: 1px solid #e74c3c;
}

.success-message {
  background-color: #d4edda;
  color: #155724;
  padding: 0.75rem;
  border-radius: 4px;
  margin-bottom: 1rem;
  border: 1px solid #c3e6cb;
}

.btn {
  width: 100%;
  padding: 0.75rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #5568d3;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-link {
  margin-top: 1.5rem;
  text-align: center;
  color: #666;
}

.login-link a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>

