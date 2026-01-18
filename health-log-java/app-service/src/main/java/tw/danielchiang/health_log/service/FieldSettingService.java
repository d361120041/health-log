package tw.danielchiang.health_log.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.danielchiang.health_log.data.repository.FieldSettingRepository;
import tw.danielchiang.health_log.model.entity.FieldSetting;

/**
 * 欄位設定服務
 * 管理記錄欄位的 CRUD 業務邏輯
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FieldSettingService {

    private final FieldSettingRepository fieldSettingRepository;

    /**
     * 查詢所有啟用的欄位設定，按 setting_id 升序排序
     * @return 啟用的欄位設定列表
     */
    @Transactional(readOnly = true)
    public List<FieldSetting> getAllActiveFieldSettings() {
        return fieldSettingRepository.findByIsActiveTrueOrderBySettingIdAsc();
    }

    /**
     * 查詢所有欄位設定（包含未啟用的），按 setting_id 升序排序
     * @return 所有欄位設定列表
     */
    @Transactional(readOnly = true)
    public List<FieldSetting> getAllFieldSettings() {
        return fieldSettingRepository.findAllByOrderBySettingIdAsc();
    }

    /**
     * 根據 ID 查詢欄位設定
     * @param settingId 欄位設定 ID
     * @return 欄位設定
     */
    @Transactional(readOnly = true)
    public Optional<FieldSetting> getFieldSettingById(Integer settingId) {
        return fieldSettingRepository.findById(settingId);
    }

    /**
     * 根據欄位名稱查詢欄位設定
     * @param fieldName 欄位名稱
     * @return 欄位設定
     */
    @Transactional(readOnly = true)
    public Optional<FieldSetting> getFieldSettingByFieldName(String fieldName) {
        return fieldSettingRepository.findByFieldName(fieldName);
    }

    /**
     * 創建欄位設定
     * @param fieldSetting 欄位設定實體
     * @return 儲存後的欄位設定
     * @throws IllegalArgumentException 如果欄位名稱已存在
     */
    public FieldSetting createFieldSetting(FieldSetting fieldSetting) {
        // 檢查欄位名稱是否已存在
        if (fieldSettingRepository.existsByFieldName(fieldSetting.getFieldName())) {
            throw new IllegalArgumentException("欄位名稱已存在: " + fieldSetting.getFieldName());
        }

        // 設定預設值
        if (fieldSetting.getIsRequired() == null) {
            fieldSetting.setIsRequired(false);
        }
        if (fieldSetting.getIsActive() == null) {
            fieldSetting.setIsActive(true);
        }

        FieldSetting saved = fieldSettingRepository.save(fieldSetting);
        log.info("Field setting created: settingId={}, fieldName={}", saved.getSettingId(), saved.getFieldName());
        return saved;
    }

    /**
     * 更新欄位設定
     * @param settingId 欄位設定 ID
     * @param fieldSetting 更新的欄位設定資料
     * @return 更新後的欄位設定
     * @throws IllegalArgumentException 如果欄位設定不存在或欄位名稱衝突
     */
    public FieldSetting updateFieldSetting(Integer settingId, FieldSetting fieldSetting) {
        FieldSetting existing = fieldSettingRepository.findById(settingId)
                .orElseThrow(() -> new IllegalArgumentException("欄位設定不存在: settingId=" + settingId));

        // 如果欄位名稱有變更，檢查新名稱是否已存在
        if (!existing.getFieldName().equals(fieldSetting.getFieldName())) {
            if (fieldSettingRepository.existsByFieldName(fieldSetting.getFieldName())) {
                throw new IllegalArgumentException("欄位名稱已存在: " + fieldSetting.getFieldName());
            }
        }

        // 更新欄位
        existing.setFieldName(fieldSetting.getFieldName());
        existing.setDataType(fieldSetting.getDataType());
        existing.setUnit(fieldSetting.getUnit());
        existing.setIsRequired(fieldSetting.getIsRequired());
        existing.setOptions(fieldSetting.getOptions());
        existing.setIsActive(fieldSetting.getIsActive());

        FieldSetting updated = fieldSettingRepository.save(existing);
        log.info("Field setting updated: settingId={}, fieldName={}", updated.getSettingId(), updated.getFieldName());
        return updated;
    }

    /**
     * 刪除欄位設定（軟刪除：設定為未啟用）
     * @param settingId 欄位設定 ID
     * @throws IllegalArgumentException 如果欄位設定不存在
     */
    public void deleteFieldSetting(Integer settingId) {
        FieldSetting fieldSetting = fieldSettingRepository.findById(settingId)
                .orElseThrow(() -> new IllegalArgumentException("欄位設定不存在: settingId=" + settingId));

        // 軟刪除：設定為未啟用
        fieldSetting.setIsActive(false);
        fieldSettingRepository.save(fieldSetting);
        log.info("Field setting deactivated: settingId={}, fieldName={}", fieldSetting.getSettingId(), fieldSetting.getFieldName());
    }

    /**
     * 硬刪除欄位設定（從資料庫中完全刪除）
     * @param settingId 欄位設定 ID
     * @throws IllegalArgumentException 如果欄位設定不存在
     */
    public void hardDeleteFieldSetting(Integer settingId) {
        if (!fieldSettingRepository.existsById(settingId)) {
            throw new IllegalArgumentException("欄位設定不存在: settingId=" + settingId);
        }

        fieldSettingRepository.deleteById(settingId);
        log.info("Field setting hard deleted: settingId={}", settingId);
    }
}

