<template>
  <form @submit.prevent="handleSubmit" class="dynamic-record-form">
    <div v-if="isLoading" class="loading-message">載入欄位設定中...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else>
      <div
        v-for="field in activeFields"
        :key="field.settingId"
        class="form-field"
      >
        <!-- NUMBER 類型 -->
        <InputNumber
          v-if="field.dataType === 'NUMBER'"
          :field-name="field.fieldName"
          :label="field.fieldName"
          :model-value="getFieldValue(field.fieldName)"
          @update:model-value="updateFieldValue(field.fieldName, $event)"
          :unit="field.unit || ''"
          :required="field.isRequired || false"
          :error="getFieldError(field.fieldName)"
        />

        <!-- TEXT 類型 -->
        <InputTextarea
          v-else-if="field.dataType === 'TEXT'"
          :field-name="field.fieldName"
          :label="field.fieldName"
          :model-value="getFieldValue(field.fieldName)"
          @update:model-value="updateFieldValue(field.fieldName, $event)"
          :required="field.isRequired || false"
          :error="getFieldError(field.fieldName)"
        />

        <!-- ENUM 類型 -->
        <InputSelect
          v-else-if="field.dataType === 'ENUM'"
          :field-name="field.fieldName"
          :label="field.fieldName"
          :model-value="getFieldValue(field.fieldName)"
          @update:model-value="updateFieldValue(field.fieldName, $event)"
          :options="parseEnumOptions(field.options)"
          :required="field.isRequired || false"
          :error="getFieldError(field.fieldName)"
        />
      </div>

      <div v-if="activeFields.length === 0" class="empty-message">
        目前沒有可用的欄位設定
      </div>

      <div class="form-actions">
        <slot name="actions" :submit="handleSubmit" :is-valid="isFormValid">
          <button
            type="submit"
            :disabled="!isFormValid || isSubmitting"
            class="btn btn-primary"
          >
            {{ isSubmitting ? '儲存中...' : '儲存' }}
          </button>
          <button
            type="button"
            @click="handleCancel"
            class="btn btn-secondary"
            :disabled="isSubmitting"
          >
            取消
          </button>
        </slot>
      </div>
    </div>
  </form>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useSettingsStore } from '@/stores/settingsStore'
import InputNumber from './InputNumber.vue'
import InputTextarea from './InputTextarea.vue'
import InputSelect from './InputSelect.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  initialValues: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:modelValue', 'submit', 'cancel'])

const settingsStore = useSettingsStore()

// 表單數據
const fieldValues = ref({})
const fieldErrors = ref({})
const isSubmitting = ref(false)

// 計算屬性
const isLoading = computed(() => settingsStore.isLoading)
const error = computed(() => settingsStore.error)
const activeFields = computed(() => {
  return settingsStore.fieldSettings.filter((field) => field.isActive)
})

const isFormValid = computed(() => {
  // 檢查所有必填欄位是否都有值
  return activeFields.value.every((field) => {
    if (!field.isRequired) return true
    const value = fieldValues.value[field.fieldName]
    return value !== undefined && value !== null && value !== ''
  })
})

// 方法
const getFieldValue = (fieldName) => {
  return fieldValues.value[fieldName] || ''
}

const updateFieldValue = (fieldName, value) => {
  fieldValues.value[fieldName] = value
  // 清除該欄位的錯誤
  if (fieldErrors.value[fieldName]) {
    delete fieldErrors.value[fieldName]
  }
  // 發送更新事件
  emit('update:modelValue', { ...fieldValues.value })
}

const getFieldError = (fieldName) => {
  return fieldErrors.value[fieldName] || ''
}

const validateForm = () => {
  fieldErrors.value = {}
  let isValid = true

  activeFields.value.forEach((field) => {
    if (field.isRequired) {
      const value = fieldValues.value[field.fieldName]
      if (value === undefined || value === null || value === '') {
        fieldErrors.value[field.fieldName] = `${field.fieldName} 為必填欄位`
        isValid = false
      }
    }
  })

  return isValid
}

const parseEnumOptions = (optionsString) => {
  if (!optionsString) return []

  try {
    // 嘗試解析為 JSON
    const parsed = JSON.parse(optionsString)
    if (Array.isArray(parsed)) {
      return parsed.map((opt) => {
        if (typeof opt === 'string') {
          return { value: opt, label: opt }
        }
        return { value: opt.value || opt, label: opt.label || opt.value || opt }
      })
    }
  } catch (e) {
    // 如果不是 JSON，嘗試用逗號分隔
    const options = optionsString.split(',').map((opt) => opt.trim())
    return options.map((opt) => ({ value: opt, label: opt }))
  }

  return []
}

const handleSubmit = () => {
  if (!validateForm()) {
    return
  }

  isSubmitting.value = true
  emit('submit', { ...fieldValues.value })
  // 注意：isSubmitting 應該由父元件在提交完成後重置
}

const handleCancel = () => {
  emit('cancel')
}

// 初始化：載入欄位設定
onMounted(async () => {
  if (settingsStore.fieldSettings.length === 0) {
    await settingsStore.fetchFieldSettings()
  }

  // 初始化表單數據
  if (props.initialValues && Object.keys(props.initialValues).length > 0) {
    fieldValues.value = { ...props.initialValues }
  } else if (props.modelValue && Object.keys(props.modelValue).length > 0) {
    fieldValues.value = { ...props.modelValue }
  }
})

// 監聽 modelValue 變化
watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue && Object.keys(newValue).length > 0) {
      fieldValues.value = { ...newValue }
    }
  },
  { deep: true }
)

// 暴露方法給父元件
defineExpose({
  validate: validateForm,
  reset: () => {
    fieldValues.value = {}
    fieldErrors.value = {}
  },
  setSubmitting: (value) => {
    isSubmitting.value = value
  },
})
</script>

<style scoped>
.dynamic-record-form {
  width: 100%;
}

.loading-message,
.error-message,
.empty-message {
  padding: 1rem;
  text-align: center;
  color: #666;
}

.error-message {
  color: #e74c3c;
  background-color: #fee;
  border: 1px solid #e74c3c;
  border-radius: 4px;
}

.form-field {
  margin-bottom: 1rem;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #eee;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: #4a90e2;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #357abd;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: #333;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #e0e0e0;
}
</style>

