<template>
  <div class="input-select-wrapper">
    <label :for="fieldName" class="input-label">
      {{ label }}
      <span v-if="required" class="required-mark">*</span>
    </label>
    <select
      :id="fieldName"
      :value="modelValue"
      @change="handleChange"
      :required="required"
      class="input-field"
      :class="{ 'input-error': error }"
    >
      <option value="" disabled>{{ placeholder || '請選擇...' }}</option>
      <option
        v-for="option in options"
        :key="option.value"
        :value="option.value"
      >
        {{ option.label }}
      </option>
    </select>
    <span v-if="error" class="error-message">{{ error }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  fieldName: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    default: '',
  },
  modelValue: {
    type: String,
    default: '',
  },
  options: {
    type: Array,
    required: true,
    validator: (value) => {
      return value.every((opt) => opt.value !== undefined && opt.label !== undefined)
    },
  },
  required: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: '',
  },
  error: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const handleChange = (event) => {
  const value = event.target.value
  emit('update:modelValue', value)
}
</script>

<style scoped>
.input-select-wrapper {
  margin-bottom: 1.5rem;
}

.input-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.required-mark {
  color: #e74c3c;
  margin-left: 0.25rem;
}

.input-field {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  font-family: inherit;
  background-color: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.input-field:focus {
  outline: none;
  border-color: #4a90e2;
}

.input-field.input-error {
  border-color: #e74c3c;
}

.error-message {
  display: block;
  margin-top: 0.25rem;
  color: #e74c3c;
  font-size: 0.875rem;
}
</style>

