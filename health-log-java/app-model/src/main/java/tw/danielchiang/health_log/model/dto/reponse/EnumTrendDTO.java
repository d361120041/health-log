package tw.danielchiang.health_log.model.dto.reponse;

import java.time.LocalDate;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENUM 類型時間序列趨勢 DTO
 * 用於 ENUM 類型欄位的時間序列分佈報表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumTrendDTO {

    /**
     * 日期 -> 選項名稱 -> 出現次數
     */
    private Map<LocalDate, Map<String, Long>> trendData;

    /**
     * 所有出現過的選項列表
     */
    private java.util.List<String> options;
}
