<template>
  <Teleport to="body">
    <div
      v-if="show && date"
      class="date-dialog"
      :style="dialogStyle"
      @click.stop
    >
      <div class="dialog-header">
        <h3>{{ formatDisplayDate(date) }}</h3>
        <button @click="handleClose" class="close-btn">×</button>
      </div>

      <div class="dialog-content">
        <div v-if="hasRecord" class="message">
          <p>此日期已有記錄</p>
        </div>
        <div v-else class="message">
          <p>此日期尚未記錄</p>
        </div>

        <div class="actions">
          <button
            v-if="hasRecord"
            @click="handleEdit"
            class="btn btn-primary"
          >
            編輯記錄
          </button>
          <button
            v-else
            @click="handleNew"
            class="btn btn-primary"
          >
            新增記錄
          </button>
          <button
            @click="handleClose"
            class="btn btn-secondary"
          >
            取消
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  date: {
    type: String,
    required: true
  },
  hasRecord: {
    type: Boolean,
    default: false
  },
  show: {
    type: Boolean,
    default: false
  },
  position: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close'])

const router = useRouter()

// 格式化顯示日期
const formatDisplayDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

// 處理新增
const handleNew = () => {
  router.push({
    name: 'RecordNew',
    query: { date: props.date }
  })
  handleClose()
}

// 處理編輯
const handleEdit = () => {
  router.push({
    name: 'RecordEdit',
    params: { date: props.date }
  })
  handleClose()
}

// 處理關閉
const handleClose = () => {
  emit('close')
}

// 計算對話框位置
const dialogStyle = computed(() => {
  if (!props.position) {
    return {
      top: '100px',
      left: '100px'
    }
  }
  
  const dialogWidth = 280
  const viewportWidth = typeof window !== 'undefined' ? window.innerWidth : 1200
  
  // 對話框頂端對齊日期頂端
  const top = props.position.top
  
  // 優先顯示在右邊
  let left = props.position.left + props.position.width + 10
  
  // 檢查右邊是否有足夠空間
  if (left + dialogWidth > viewportWidth - 10) {
    // 右邊空間不足，顯示在左邊
    left = props.position.left - dialogWidth - 10
    
    // 如果左邊也不夠，則貼著日期左邊顯示
    if (left < 10) {
      left = props.position.left + props.position.width + 10
      // 如果右邊也不夠，則調整到視窗內
      if (left + dialogWidth > viewportWidth - 10) {
        left = viewportWidth - dialogWidth - 10
      }
    }
  }
  
  return {
    top: `${top}px`,
    left: `${left}px`
  }
})
</script>

<style scoped>
.date-dialog {
  position: fixed;
  background: white;
  border-radius: 8px;
  width: 280px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 200px;
  border: 1px solid #e0e0e0;
  margin: 0;
  padding: 0;
  transform: none;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #eee;
}

.dialog-header h3 {
  margin: 0;
  color: #333;
  font-size: 0.95rem;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.dialog-content {
  padding: 1rem;
}

.message {
  text-align: center;
  margin-bottom: 1rem;
}

.message p {
  margin: 0;
  color: #666;
  font-size: 0.85rem;
}

.actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #4a90e2;
  color: white;
}

.btn-primary:hover {
  background-color: #357abd;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: #333;
}

.btn-secondary:hover {
  background-color: #e0e0e0;
}
</style>
