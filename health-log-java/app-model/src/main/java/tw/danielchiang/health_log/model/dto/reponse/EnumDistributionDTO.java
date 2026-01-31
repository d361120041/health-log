package tw.danielchiang.health_log.model.dto.reponse;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENUM 類型分佈統計 DTO
 * 用於 ENUM 類型欄位的分類統計報表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumDistributionDTO {

    /**
     * 選項名稱與出現次數的對應
     */
    private Map<String, Long> distribution;

    /**
     * 總記錄數
     */
    private Long totalCount;

    /**
     * 各選項的百分比（選項名稱 -> 百分比）
     */
    private Map<String, Double> percentages;
}
