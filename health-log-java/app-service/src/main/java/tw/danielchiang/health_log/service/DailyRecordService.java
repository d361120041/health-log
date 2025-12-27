package tw.danielchiang.health_log.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.danielchiang.health_log.data.repository.DailyRecordRepository;
import tw.danielchiang.health_log.data.repository.FieldSettingRepository;
import tw.danielchiang.health_log.data.repository.RecordDataRepository;
import tw.danielchiang.health_log.data.repository.UserRepository;
import tw.danielchiang.health_log.model.dto.DailyRecordDetailDTO;
import tw.danielchiang.health_log.model.dto.RecordRequestDTO;
import tw.danielchiang.health_log.model.entity.DailyRecord;
import tw.danielchiang.health_log.model.entity.FieldSetting;
import tw.danielchiang.health_log.model.entity.RecordData;
import tw.danielchiang.health_log.model.entity.User;

/**
 * 每日記錄服務
 * 負責 EAV 模式的寫入（拆解 DTO）和查詢（彙整 DTO）邏輯
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DailyRecordService {

    private final DailyRecordRepository dailyRecordRepository;
    private final RecordDataRepository recordDataRepository;
    private final FieldSettingRepository fieldSettingRepository;
    private final UserRepository userRepository;

    /**
     * 創建或更新每日記錄
     * 將扁平化的 DTO 拆解為 EAV 格式儲存
     * @param userId 使用者 ID
     * @param request 記錄請求 DTO
     * @return 記錄詳情 DTO
     * @throws IllegalArgumentException 如果使用者不存在、欄位設定不存在或驗證失敗
     */
    public DailyRecordDetailDTO saveRecord(Long userId, RecordRequestDTO request) {
        // 驗證使用者存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("使用者不存在: userId=" + userId));

        // 查詢或創建 DailyRecord
        DailyRecord dailyRecord = dailyRecordRepository
                .findByUserIdAndRecordDate(userId, request.getRecordDate())
                .orElseGet(() -> {
                    DailyRecord newRecord = new DailyRecord();
                    newRecord.setUser(user);
                    newRecord.setRecordDate(request.getRecordDate());
                    return dailyRecordRepository.save(newRecord);
                });

        // 獲取所有啟用的欄位設定
        List<FieldSetting> activeSettings = fieldSettingRepository.findByIsActiveTrue();
        Map<String, FieldSetting> settingMap = activeSettings.stream()
                .collect(Collectors.toMap(FieldSetting::getFieldName, fs -> fs));

        // 驗證並處理欄位值
        if (request.getFieldValues() != null) {
            // 刪除現有的 RecordData（如果存在）
            List<RecordData> existingData = recordDataRepository.findByDailyRecordRecordId(dailyRecord.getRecordId());
            recordDataRepository.deleteAll(existingData);

            // 創建新的 RecordData
            for (Map.Entry<String, String> entry : request.getFieldValues().entrySet()) {
                String fieldName = entry.getKey();
                String value = entry.getValue();

                FieldSetting fieldSetting = settingMap.get(fieldName);
                if (fieldSetting == null) {
                    log.warn("欄位設定不存在，跳過: fieldName={}", fieldName);
                    continue;
                }

                // 驗證必填欄位
                if (fieldSetting.getIsRequired() && (value == null || value.trim().isEmpty())) {
                    throw new IllegalArgumentException("必填欄位不能為空: fieldName=" + fieldName);
                }

                // 創建 RecordData
                RecordData recordData = new RecordData();
                recordData.setDailyRecord(dailyRecord);
                recordData.setFieldSetting(fieldSetting);
                recordData.setValueText(value != null ? value : "");
                recordDataRepository.save(recordData);
            }
        }

        // 重新載入以獲取最新的 RecordData
        dailyRecord = dailyRecordRepository.findById(dailyRecord.getRecordId())
                .orElseThrow(() -> new IllegalStateException("記錄儲存後無法查詢"));

        log.info("Record saved: userId={}, recordDate={}, recordId={}", userId, request.getRecordDate(), dailyRecord.getRecordId());
        return convertToDetailDTO(dailyRecord);
    }

    /**
     * 根據日期查詢單日記錄詳情
     * 將 EAV 格式彙整為扁平化的 DTO
     * @param userId 使用者 ID
     * @param recordDate 記錄日期
     * @return 記錄詳情 DTO，如果不存在則返回 Optional.empty()
     */
    @Transactional(readOnly = true)
    public Optional<DailyRecordDetailDTO> getRecordByDate(Long userId, LocalDate recordDate) {
        Optional<DailyRecord> dailyRecordOpt = dailyRecordRepository
                .findByUserIdAndRecordDate(userId, recordDate);

        if (dailyRecordOpt.isEmpty()) {
            return Optional.empty();
        }

        DailyRecord dailyRecord = dailyRecordOpt.get();
        return Optional.of(convertToDetailDTO(dailyRecord));
    }

    /**
     * 查詢使用者的所有記錄（按日期降序）
     * @param userId 使用者 ID
     * @return 記錄詳情 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<DailyRecordDetailDTO> getAllRecordsByUserId(Long userId) {
        List<DailyRecord> dailyRecords = dailyRecordRepository.findByUserIdOrderByRecordDateDesc(userId);
        return dailyRecords.stream()
                .map(this::convertToDetailDTO)
                .collect(Collectors.toList());
    }

    /**
     * 刪除記錄
     * @param userId 使用者 ID
     * @param recordDate 記錄日期
     * @throws IllegalArgumentException 如果記錄不存在或不屬於該使用者
     */
    public void deleteRecord(Long userId, LocalDate recordDate) {
        DailyRecord dailyRecord = dailyRecordRepository
                .findByUserIdAndRecordDate(userId, recordDate)
                .orElseThrow(() -> new IllegalArgumentException("記錄不存在: userId=" + userId + ", recordDate=" + recordDate));

        dailyRecordRepository.delete(dailyRecord);
        log.info("Record deleted: userId={}, recordDate={}, recordId={}", userId, recordDate, dailyRecord.getRecordId());
    }

    /**
     * 將 DailyRecord 實體轉換為 DailyRecordDetailDTO
     * 執行 EAV -> 扁平化的轉換
     */
    private DailyRecordDetailDTO convertToDetailDTO(DailyRecord dailyRecord) {
        DailyRecordDetailDTO dto = new DailyRecordDetailDTO();
        dto.setRecordId(dailyRecord.getRecordId());
        dto.setRecordDate(dailyRecord.getRecordDate());
        dto.setCreatedAt(dailyRecord.getCreatedAt());

        // 彙整 RecordData 為 Map
        Map<String, String> fieldValues = new HashMap<>();
        if (dailyRecord.getRecordDataList() != null) {
            for (RecordData recordData : dailyRecord.getRecordDataList()) {
                String fieldName = recordData.getFieldSetting().getFieldName();
                String value = recordData.getValueText();
                fieldValues.put(fieldName, value);
            }
        }
        dto.setFieldValues(fieldValues);

        return dto;
    }
}

