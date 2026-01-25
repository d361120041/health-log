package tw.danielchiang.health_log.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.danielchiang.health_log.data.repository.DailyRecordRepository;
import tw.danielchiang.health_log.data.repository.FieldSettingRepository;
import tw.danielchiang.health_log.data.repository.RecordDataRepository;
import tw.danielchiang.health_log.model.dto.reponse.TrendDataPointDTO;
import tw.danielchiang.health_log.model.entity.DailyRecord;
import tw.danielchiang.health_log.model.entity.FieldSetting;
import tw.danielchiang.health_log.model.entity.RecordData;

/**
 * 報告服務
 * 負責執行複雜的趨勢數據查詢
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final DailyRecordRepository dailyRecordRepository;
    private final RecordDataRepository recordDataRepository;
    private final FieldSettingRepository fieldSettingRepository;

    /**
     * 查詢特定欄位在一段時間內的趨勢數據
     * @param userId 使用者 ID
     * @param fieldName 欄位名稱
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 趨勢數據點列表（按日期升序）
     * @throws IllegalArgumentException 如果欄位設定不存在
     */
    public List<TrendDataPointDTO> getTrendData(Long userId, String fieldName, LocalDate startDate, LocalDate endDate) {
        // 驗證欄位設定存在
        FieldSetting fieldSetting = fieldSettingRepository.findByFieldName(fieldName)
                .orElseThrow(() -> new IllegalArgumentException("欄位設定不存在: fieldName=" + fieldName));

        // 查詢指定日期範圍內的記錄
        List<DailyRecord> dailyRecords = dailyRecordRepository.findByUserIdOrderByRecordDateDesc(userId)
                .stream()
                .filter(record -> !record.getRecordDate().isBefore(startDate) && !record.getRecordDate().isAfter(endDate))
                .collect(Collectors.toList());

        // 查詢每個記錄對應的欄位值
        return dailyRecords.stream()
                .map(record -> {
                    // 查詢該記錄的欄位值
                    Optional<RecordData> recordDataOpt = recordDataRepository
                            .findBySettingIdAndRecordId(fieldSetting.getSettingId(), record.getRecordId());

                    if (recordDataOpt.isPresent()) {
                        RecordData recordData = recordDataOpt.get();
                        return new TrendDataPointDTO(record.getRecordDate(), recordData.getValueText());
                    } else {
                        // 如果該日期沒有該欄位的值，返回 null（後續過濾掉）
                        return null;
                    }
                })
                .filter(dto -> dto != null) // 過濾掉沒有值的記錄
                .sorted((a, b) -> a.getDate().compareTo(b.getDate())) // 按日期升序排序
                .collect(Collectors.toList());
    }

    /**
     * 查詢特定欄位在指定日期範圍內的所有數據點（包含空值）
     * @param userId 使用者 ID
     * @param fieldName 欄位名稱
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 趨勢數據點列表（按日期升序，包含空值）
     * @throws IllegalArgumentException 如果欄位設定不存在
     */
    public List<TrendDataPointDTO> getTrendDataWithNulls(Long userId, String fieldName, LocalDate startDate, LocalDate endDate) {
        // 驗證欄位設定存在
        FieldSetting fieldSetting = fieldSettingRepository.findByFieldName(fieldName)
                .orElseThrow(() -> new IllegalArgumentException("欄位設定不存在: fieldName=" + fieldName));

        // 查詢指定日期範圍內的記錄
        List<DailyRecord> dailyRecords = dailyRecordRepository.findByUserIdOrderByRecordDateDesc(userId)
                .stream()
                .filter(record -> !record.getRecordDate().isBefore(startDate) && !record.getRecordDate().isAfter(endDate))
                .collect(Collectors.toList());

        // 查詢每個記錄對應的欄位值
        return dailyRecords.stream()
                .map(record -> {
                    // 查詢該記錄的欄位值
                    Optional<RecordData> recordDataOpt = recordDataRepository
                            .findBySettingIdAndRecordId(fieldSetting.getSettingId(), record.getRecordId());

                    if (recordDataOpt.isPresent()) {
                        RecordData recordData = recordDataOpt.get();
                        return new TrendDataPointDTO(record.getRecordDate(), recordData.getValueText());
                    } else {
                        // 如果該日期沒有該欄位的值，返回空字串
                        return new TrendDataPointDTO(record.getRecordDate(), "");
                    }
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate())) // 按日期升序排序
                .collect(Collectors.toList());
    }
}

