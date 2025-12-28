<template>
  <div class="input-textarea-wrapper">
    <label :for="fieldName" class="input-label">
      {{ label }}
      <span v-if="required" class="required-mark">*</span>
    </label>
    <textarea
      :id="fieldName"
      :value="modelValue"
      @input="handleInput"
      :required="required"
      :placeholder="placeholder"
      class="input-field"
      :class="{ 'input-error': error }"
      rows="4"
    ></textarea>
    <span v-if="error" class="error-message">{{ error }}</span>
  </div>
</template>

<script setup>
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

const handleInput = (event) => {
  const value = event.target.value
  emit('update:modelValue', value)
}
</script>

<style scoped>
.input-textarea-wrapper {
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
  resize: vertical;
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

