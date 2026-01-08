<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import NavBar from '@/components/common/NavBar.vue'

const route = useRoute()
const authStore = useAuthStore()

// 判斷是否應該顯示導航欄
// 只在已登入且不在認證頁面時顯示
const showNavBar = computed(() => {
  const authPages = ['Login', 'Register', 'VerifyEmail']
  return authStore.isAuthenticated && !authPages.includes(route.name)
})
</script>

<template>
  <div id="app">
    <NavBar v-if="showNavBar" />
    <router-view />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
    Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #333;
  background-color: #f5f5f5;
}

#app {
  min-height: 100vh;
}
</style>
