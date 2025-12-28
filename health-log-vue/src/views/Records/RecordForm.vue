<template>
  <div class="record-form-container">
    <div class="header">
      <h1>{{ isEditMode ? '編輯記錄' : '新增記錄' }}</h1>
      <router-link to="/records" class="btn btn-secondary">返回列表</router-link>
    </div>

    <div v-if="isLoadingRecord" class="loading-message">載入記錄中...</div>
    <div v-else-if="recordError" class="error-message">{{ recordError }}</div>
    <div v-else class="form-wrapper">
      <div class="date-selector">
        <label for="record-date">記錄日期</label>
        <input
          id="record-date"
          v-model="selectedDate"
          type="date"
          class="date-input"
          :disabled="isEditMode"
        />
      </div>

      <DynamicRecordForm
        :initial-values="initialFieldValues"
        @submit="handleSubmit"
        @cancel="handleCancel"
        ref="formRef"
      >
        <template #actions="{ submit, isValid }">
          <button
            type="button"
            @click="submit"
            :disabled="!isValid || isSaving"
            class="btn btn-primary"
          >
            {{ isSaving ? '儲存中...' : '儲存' }}
          </button>
          <button
            type="button"
            @click="handleCancel"
            class="btn btn-secondary"
            :disabled="isSaving"
          >
            取消
          </button>
        </template>
      </DynamicRecordForm>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecordsStore } from '@/stores/recordsStore'
import DynamicRecordForm from '@/components/dynamic/DynamicRecordForm.vue'

const route = useRoute()
const router = useRouter()
const recordsStore = useRecordsStore()

const formRef = ref(null)
const selectedDate = ref('')
const initialFieldValues = ref({})
const isSaving = ref(false)
const isLoadingRecord = ref(false)
const recordError = ref('')

const isEditMode = computed(() => !!route.params.date)

// 初始化日期
const initDate = () => {
  if (isEditMode.value) {
    selectedDate.value = route.params.date
  } else {
    // 預設為今天
    const today = new Date()
    selectedDate.value = today.toISOString().split('T')[0]
  }
}

// 載入記錄數據（編輯模式）
const loadRecord = async () => {
  if (!isEditMode.value) {
    initialFieldValues.value = {}
    return
  }

  isLoadingRecord.value = true
  recordError.value = ''

  try {
    const record = await recordsStore.fetchRecordByDate(route.params.date)
    if (record) {
      initialFieldValues.value = record.fieldValues || {}
    } else {
      initialFieldValues.value = {}
    }
  } catch (err) {
    recordError.value = '載入記錄失敗，請稍後再試'
    console.error('Load record error:', err)
  } finally {
    isLoadingRecord.value = false
  }
}

const handleSubmit = async (fieldValues) => {
  if (!selectedDate.value) {
    alert('請選擇記錄日期')
    return
  }

  isSaving.value = true
  if (formRef.value) {
    formRef.value.setSubmitting(true)
  }

  try {
    await recordsStore.saveRecord({
      recordDate: selectedDate.value,
      fieldValues,
    })

    // 儲存成功，返回列表
    router.push('/records')
  } catch (err) {
    console.error('Save record error:', err)
    alert('儲存失敗，請稍後再試')
  } finally {
    isSaving.value = false
    if (formRef.value) {
      formRef.value.setSubmitting(false)
    }
  }
}

const handleCancel = () => {
  router.push('/records')
}

onMounted(async () => {
  initDate()
  await loadRecord()
})
</script>

<style scoped>
.record-form-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.header h1 {
  margin: 0;
  color: #333;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #4a90e2;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #357abd;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: #333;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #e0e0e0;
}

.loading-message,
.error-message {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error-message {
  color: #e74c3c;
  background-color: #fee;
  border: 1px solid #e74c3c;
  border-radius: 4px;
}

.form-wrapper {
  background: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.date-selector {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
}

.date-selector label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.date-input {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  width: 100%;
  max-width: 300px;
}

.date-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}
</style>

