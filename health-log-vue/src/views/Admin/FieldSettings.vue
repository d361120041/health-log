<template>
  <div class="field-settings-container">
    <div class="header">
      <h1>欄位設定管理</h1>
      <button @click="showAddModal = true" class="btn btn-primary">
        新增欄位
      </button>
    </div>

    <div v-if="isLoading" class="loading-message">載入中...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else>
      <div v-if="allFieldSettings.length === 0" class="empty-message">
        目前沒有欄位設定
      </div>
      <div v-else class="settings-table">
        <table>
          <thead>
            <tr>
              <th>欄位名稱</th>
              <th>資料類型</th>
              <th>單位</th>
              <th>必填</th>
              <th>狀態</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="setting in allFieldSettings" :key="setting.settingId">
              <td>{{ setting.fieldName }}</td>
              <td>{{ setting.dataType }}</td>
              <td>{{ setting.unit || '-' }}</td>
              <td>{{ setting.isRequired ? '是' : '否' }}</td>
              <td>
                <span :class="setting.isActive ? 'status-active' : 'status-inactive'">
                  {{ setting.isActive ? '啟用' : '停用' }}
                </span>
              </td>
              <td>
                <button
                  @click="handleEdit(setting)"
                  class="btn-icon"
                  title="編輯"
                >
                  ✏️
                </button>
                <button
                  @click="handleDelete(setting.settingId)"
                  class="btn-icon btn-danger"
                  title="刪除"
                  :disabled="isDeleting"
                >
                  🗑️
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增/編輯 Modal -->
    <div v-if="showAddModal || editingSetting" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h2>{{ editingSetting ? '編輯欄位' : '新增欄位' }}</h2>
        <form @submit.prevent="handleSaveSetting">
          <div class="form-group">
            <label>欄位名稱 *</label>
            <input
              v-model="formData.fieldName"
              type="text"
              required
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label>資料類型 *</label>
            <select v-model="formData.dataType" required class="form-select">
              <option value="NUMBER">數字 (NUMBER)</option>
              <option value="TEXT">文字 (TEXT)</option>
              <option value="ENUM">選項 (ENUM)</option>
            </select>
          </div>

          <div class="form-group">
            <label>單位</label>
            <input
              v-model="formData.unit"
              type="text"
              class="form-input"
              placeholder="例如：kg, cm, 次"
            />
          </div>

          <div class="form-group">
            <label>
              <input
                v-model="formData.isRequired"
                type="checkbox"
              />
              必填欄位
            </label>
          </div>

          <div v-if="formData.dataType === 'ENUM'" class="form-group">
            <label>選項列表 *</label>
            <textarea
              v-model="formData.options"
              class="form-textarea"
              placeholder='JSON 格式：["選項1", "選項2"] 或逗號分隔：選項1,選項2'
              required
            ></textarea>
          </div>

          <div class="form-group">
            <label>
              <input
                v-model="formData.isActive"
                type="checkbox"
              />
              啟用
            </label>
          </div>

          <div class="modal-actions">
            <button type="submit" class="btn btn-primary" :disabled="isSaving">
              {{ isSaving ? '儲存中...' : '儲存' }}
            </button>
            <button type="button" @click="closeModal" class="btn btn-secondary">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useSettingsStore } from '@/stores/settingsStore'

const settingsStore = useSettingsStore()

const showAddModal = ref(false)
const editingSetting = ref(null)
const isSaving = ref(false)
const isDeleting = ref(false)

const formData = ref({
  fieldName: '',
  dataType: 'NUMBER',
  unit: '',
  isRequired: false,
  options: '',
  isActive: true,
})

const allFieldSettings = computed(() => settingsStore.fieldSettings)
const isLoading = computed(() => settingsStore.isLoading)
const error = computed(() => settingsStore.error)

const resetForm = () => {
  formData.value = {
    fieldName: '',
    dataType: 'NUMBER',
    unit: '',
    isRequired: false,
    options: '',
    isActive: true,
  }
  editingSetting.value = null
  showAddModal.value = false
}

const handleEdit = (setting) => {
  editingSetting.value = setting
  formData.value = {
    fieldName: setting.fieldName,
    dataType: setting.dataType,
    unit: setting.unit || '',
    isRequired: setting.isRequired || false,
    options: setting.options || '',
    isActive: setting.isActive !== false,
  }
}

const handleSaveSetting = async () => {
  isSaving.value = true

  try {
    const data = {
      fieldName: formData.value.fieldName,
      dataType: formData.value.dataType,
      unit: formData.value.unit || null,
      isRequired: formData.value.isRequired,
      options: formData.value.dataType === 'ENUM' ? formData.value.options : null,
      isActive: formData.value.isActive,
    }

    if (editingSetting.value) {
      await settingsStore.updateFieldSetting(editingSetting.value.settingId, data)
    } else {
      await settingsStore.createFieldSetting(data)
    }

    resetForm()
  } catch (err) {
    console.error('Save setting error:', err)
    alert('儲存失敗，請稍後再試')
  } finally {
    isSaving.value = false
  }
}

const handleDelete = async (id) => {
  if (!confirm('確定要刪除這個欄位設定嗎？')) {
    return
  }

  isDeleting.value = true
  try {
    await settingsStore.deleteFieldSetting(id)
  } catch (err) {
    console.error('Delete setting error:', err)
    alert('刪除失敗，請稍後再試')
  } finally {
    isDeleting.value = false
  }
}

const closeModal = () => {
  resetForm()
}

onMounted(async () => {
  if (settingsStore.fieldSettings.length === 0) {
    await settingsStore.fetchAllFieldSettings()
  }
})
</script>

<style scoped>
.field-settings-container {
  max-width: 1200px;
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

.btn-secondary:hover {
  background-color: #e0e0e0;
}

.loading-message,
.empty-message,
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

.settings-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background-color: #f5f5f5;
}

th,
td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  font-weight: 500;
  color: #333;
}

.status-active {
  color: #27ae60;
  font-weight: 500;
}

.status-inactive {
  color: #e74c3c;
  font-weight: 500;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 1.25rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  transition: transform 0.2s;
}

.btn-icon:hover:not(:disabled) {
  transform: scale(1.1);
}

.btn-icon:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  padding: 2rem;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: #333;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-textarea {
  min-height: 100px;
  font-family: inherit;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #4a90e2;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
}
</style>

