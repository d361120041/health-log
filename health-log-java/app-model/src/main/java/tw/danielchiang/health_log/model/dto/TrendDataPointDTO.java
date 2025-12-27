package tw.danielchiang.health_log.model.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趨勢數據點 DTO
 * 用於趨勢圖數據查詢
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataPointDTO {

    private LocalDate date;
    private String value; // 數值以字串格式儲存，前端可根據 dataType 轉換
}

