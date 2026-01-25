package tw.danielchiang.health_log.model.dto.request;

import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 記錄請求 DTO
 * 用於創建或更新每日記錄
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordRequestDTO {

    @NotNull(message = "記錄日期不能為空")
    private LocalDate recordDate;

    /**
     * 欄位值對應表
     * Key: fieldName (欄位名稱)
     * Value: 欄位值 (字串格式，後端會根據 dataType 進行驗證)
     */
    private Map<String, String> fieldValues;
}

