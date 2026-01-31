package tw.danielchiang.health_log.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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
import tw.danielchiang.health_log.model.dto.reponse.EnumDistributionDTO;
import tw.danielchiang.health_log.model.dto.reponse.EnumTrendDTO;
import tw.danielchiang.health_log.model.dto.reponse.NumberReportDTO;
import tw.danielchiang.health_log.model.dto.reponse.NumberStatisticsDTO;
import tw.danielchiang.health_log.model.dto.reponse.TextAnalysisDTO;
import tw.danielchiang.health_log.model.dto.reponse.TrendDataPointDTO;
import tw.danielchiang.health_log.model.entity.DailyRecord;
import tw.danielchiang.health_log.model.entity.FieldSetting;
import tw.danielchiang.health_log.model.entity.RecordData;

/**
 * 報告服務
 * 負責執行複雜的趨勢數據查詢
 * 支援 NUMBER, ENUM, TEXT 三種類型的報表
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final DailyRecordRepository dailyRecordRepository;
    private final RecordDataRepository recordDataRepository;
    private final FieldSettingRepository fieldSettingRepository;

    // ==================== 共享基礎方法 ====================

    /**
     * 驗證並獲取欄位設定
     * @param fieldName 欄位名稱
     * @return 欄位設定
     * @throws IllegalArgumentException 如果欄位設定不存在
     */
    protected FieldSetting validateAndGetFieldSetting(String fieldName) {
        return fieldSettingRepository.findByFieldName(fieldName)
                .orElseThrow(() -> new IllegalArgumentException("欄位設定不存在: fieldName=" + fieldName));
    }

    /**
     * 獲取指定日期範圍內的記錄
     * @param userId 使用者 ID
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 記錄列表（按日期降序）
     */
    protected List<DailyRecord> getRecordsInDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return dailyRecordRepository.findByUserIdOrderByRecordDateDesc(userId)
                .stream()
                .filter(record -> !record.getRecordDate().isBefore(startDate) 
                        && !record.getRecordDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    /**
     * 獲取記錄的欄位值
     * @param fieldSetting 欄位設定
     * @param record 記錄
     * @return 欄位值（如果存在）
     */
    protected Optional<String> getFieldValue(FieldSetting fieldSetting, DailyRecord record) {
        Optional<RecordData> recordDataOpt = recordDataRepository
                .findBySettingIdAndRecordId(fieldSetting.getSettingId(), record.getRecordId());
        return recordDataOpt.map(RecordData::getValueText);
    }

    // ==================== NUMBER 類型報表 ====================

    /**
     * 查詢 NUMBER 類型欄位的完整報表（包含趨勢和統計）
     * @param userId 使用者 ID
     * @param fieldName 欄位名稱
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return NUMBER 報表 DTO
     */
    public NumberReportDTO getNumberReport(Long userId, String fieldName, LocalDate startDate, LocalDate endDate) {
        FieldSetting fieldSetting = validateAndGetFieldSetting(fieldName);
        
        if (!"NUMBER".equals(fieldSetting.getDataType())) {
            throw new IllegalArgumentException("欄位類型不是 NUMBER: " + fieldSetting.getDataType());
        }

        List<TrendDataPointDTO> trendData = getTrendData(userId, fieldName, startDate, endDate);
        NumberStatisticsDTO statistics = calculateNumberStatistics(trendData);

        return new NumberReportDTO(trendData, statistics);
    }

    /**
     * 查詢特定欄位在一段時間內的趨勢數據（NUMBER 類型）
     * @param userId 使用者 ID
     * @param fieldName 欄位名稱
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 趨勢數據點列表（按日期升序）
     * @throws IllegalArgumentException 如果欄位設定不存在
     */
    public List<TrendDataPointDTO> getTrendData(Long userId, String fieldName, LocalDate startDate, LocalDate endDate) {
        FieldSetting fieldSetting = validateAndGetFieldSetting(fieldName);
        List<DailyRecord> dailyRecords = getRecordsInDateRange(userId, startDate, endDate);

        // 查詢每個記錄對應的欄位值
        return dailyRecords.stream()
                .map(record -> {
                    Optional<String> valueOpt = getFieldValue(fieldSetting, record);
                    return valueOpt.map(value -> new TrendDataPointDTO(record.getRecordDate(), value))
                            .orElse(null);
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
        FieldSetting fieldSetting = validateAndGetFieldSetting(fieldName);
        List<DailyRecord> dailyRecords = getRecordsInDateRange(userId, startDate, endDate);

        // 查詢每個記錄對應的欄位值
        return dailyRecords.stream()
                .map(record -> {
                    Optional<String> valueOpt = getFieldValue(fieldSetting, record);
                    String value = valueOpt.orElse("");
                    return new TrendDataPointDTO(record.getRecordDate(), value);
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate())) // 按日期升序排序
                .collect(Collectors.toList());
    }

    /**
     * 計算 NUMBER 類型的統計摘要
     * @param trendData 趨勢數據
     * @return 統計摘要
     */
    private NumberStatisticsDTO calculateNumberStatistics(List<TrendDataPointDTO> trendData) {
        if (trendData == null || trendData.isEmpty()) {
            return new NumberStatisticsDTO(null, null, null, null, 0L, null, null);
        }

        List<BigDecimal> values = trendData.stream()
                .map(dto -> {
                    try {
                        return new BigDecimal(dto.getValue());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(v -> v != null)
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            return new NumberStatisticsDTO(null, null, null, null, 0L, null, null);
        }

        BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        BigDecimal max = values.stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal min = values.stream().min(BigDecimal::compareTo).orElse(null);
        Long count = (long) values.size();

        // 計算標準差
        BigDecimal variance = values.stream()
                .map(v -> v.subtract(average).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        BigDecimal standardDeviation = new BigDecimal(Math.sqrt(variance.doubleValue()))
                .setScale(2, RoundingMode.HALF_UP);

        // 計算中位數
        List<BigDecimal> sortedValues = new ArrayList<>(values);
        sortedValues.sort(BigDecimal::compareTo);
        BigDecimal median;
        int size = sortedValues.size();
        if (size % 2 == 0) {
            median = sortedValues.get(size / 2 - 1)
                    .add(sortedValues.get(size / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            median = sortedValues.get(size / 2);
        }

        return new NumberStatisticsDTO(average, max, min, sum, count, standardDeviation, median);
    }

    // ==================== ENUM 類型報表 ====================

    /**
     * 獲取 ENUM 類型欄位的分佈統計
     * @param userId 使用者 ID
     * @param fieldName 欄位名稱
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return ENUM 分佈統計 DTO
     */
    public EnumDistributionDTO getEnumDistribution(Long userId, String fieldName, LocalDate startDate, LocalDate endDate) {
        FieldSetting fieldSetting = validateAndGetFieldSetting(fieldName);
        
        if (!"ENUM".equals(fieldSetting.getDataType())) {
            throw new IllegalArgumentException("欄位類型不是 ENUM: " + fieldSetting.getDataType());
        }

        List<DailyRecord> dailyRecords = getRecordsInDateRange(userId, startDate, endDate);

        // 統計各選項的出現次數
        Map<String, Long> distribution = new HashMap<>();
        long totalCount = 0;

        for (DailyRecord record : dailyRecords) {
            Optional<String> valueOpt = getFieldValue(fieldSetting, record);
            if (valueOpt.isPresent() && !valueOpt.get().isEmpty()) {
                String value = valueOpt.get();
                distribution.put(value, distribution.getOrDefault(value, 0L) + 1);
                totalCount++;
            }
        }

        // 計算百分比
        Map<String, Double> percentages = new HashMap<>();
        if (totalCount > 0) {
            for (Map.Entry<String, Long> entry : distribution.entrySet()) {
                double percentage = (entry.getValue().doubleValue() / totalCount) * 100.0;
                percentages.put(entry.getKey(), Math.round(percentage * 100.0) / 100.0);
            }
        }

        return new EnumDistributionDTO(distribution, totalCount, percentages);
    }

    /**
     * 獲取 ENUM 類型欄位的時間序列趨勢
     * @param userId 使用者 ID
     * @param fieldName 欄位名稱
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return ENUM 趨勢 DTO
     */
    public EnumTrendDTO getEnumTrend(Long userId, String fieldName, LocalDate startDate, LocalDate endDate) {
        FieldSetting fieldSetting = validateAndGetFieldSetting(fieldName);
        
        if (!"ENUM".equals(fieldSetting.getDataType())) {
            throw new IllegalArgumentException("欄位類型不是 ENUM: " + fieldSetting.getDataType());
        }

        List<DailyRecord> dailyRecords = getRecordsInDateRange(userId, startDate, endDate);

        // 按日期分組統計
        Map<LocalDate, Map<String, Long>> trendData = new HashMap<>();
        List<String> allOptions = new ArrayList<>();

        for (DailyRecord record : dailyRecords) {
            Optional<String> valueOpt = getFieldValue(fieldSetting, record);
            if (valueOpt.isPresent() && !valueOpt.get().isEmpty()) {
                String value = valueOpt.get();
                LocalDate date = record.getRecordDate();

                trendData.putIfAbsent(date, new HashMap<>());
                Map<String, Long> dateDistribution = trendData.get(date);
                dateDistribution.put(value, dateDistribution.getOrDefault(value, 0L) + 1);

                if (!allOptions.contains(value)) {
                    allOptions.add(value);
                }
            }
        }

        return new EnumTrendDTO(trendData, allOptions);
    }

    // ==================== TEXT 類型報表 ====================

    /**
     * 獲取 TEXT 類型欄位的文字分析報表
     * @param userId 使用者 ID
     * @param fieldName 欄位名稱
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return TEXT 分析 DTO
     */
    public TextAnalysisDTO getTextAnalysis(Long userId, String fieldName, LocalDate startDate, LocalDate endDate) {
        FieldSetting fieldSetting = validateAndGetFieldSetting(fieldName);
        
        if (!"TEXT".equals(fieldSetting.getDataType())) {
            throw new IllegalArgumentException("欄位類型不是 TEXT: " + fieldSetting.getDataType());
        }

        List<DailyRecord> dailyRecords = getRecordsInDateRange(userId, startDate, endDate);

        Map<String, Long> keywordFrequency = new HashMap<>();
        Map<String, String> timelineData = new HashMap<>();
        List<Integer> lengths = new ArrayList<>();
        long totalCount = 0;

        for (DailyRecord record : dailyRecords) {
            Optional<String> valueOpt = getFieldValue(fieldSetting, record);
            if (valueOpt.isPresent() && !valueOpt.get().isEmpty()) {
                String text = valueOpt.get();
                totalCount++;
                
                // 記錄時間序列
                timelineData.put(record.getRecordDate().toString(), text);
                
                // 記錄長度
                lengths.add(text.length());
                
                // 簡單的關鍵字提取（以空格和標點符號分隔）
                String[] words = text.toLowerCase()
                        .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ")
                        .split("\\s+");
                
                for (String word : words) {
                    if (word.length() > 1) { // 過濾單字符
                        keywordFrequency.put(word, keywordFrequency.getOrDefault(word, 0L) + 1);
                    }
                }
            }
        }

        // 計算統計值
        Double averageLength = lengths.isEmpty() ? 0.0 
                : lengths.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        Integer maxLength = lengths.isEmpty() ? 0 
                : lengths.stream().mapToInt(Integer::intValue).max().orElse(0);
        Integer minLength = lengths.isEmpty() ? 0 
                : lengths.stream().mapToInt(Integer::intValue).min().orElse(0);

        return new TextAnalysisDTO(keywordFrequency, totalCount, averageLength, maxLength, minLength, timelineData);
    }
}

