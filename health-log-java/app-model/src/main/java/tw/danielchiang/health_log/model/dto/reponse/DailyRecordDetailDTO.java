package tw.danielchiang.health_log.model.dto.reponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每日記錄詳情 DTO
 * 用於查詢單日記錄，包含所有欄位值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyRecordDetailDTO {

    private Long recordId;
    private LocalDate recordDate;
    private OffsetDateTime createdAt;

    /**
     * 欄位值對應表
     * Key: fieldName (欄位名稱)
     * Value: 欄位值 (字串格式)
     */
    private Map<String, String> fieldValues;
}

