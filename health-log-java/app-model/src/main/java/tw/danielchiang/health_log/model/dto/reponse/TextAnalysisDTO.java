package tw.danielchiang.health_log.model.dto.reponse;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TEXT 類型分析報表 DTO
 * 用於 TEXT 類型欄位的文字分析報表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextAnalysisDTO {

    /**
     * 關鍵字頻率統計（關鍵字 -> 出現次數）
     */
    private Map<String, Long> keywordFrequency;

    /**
     * 總記錄數
     */
    private Long totalCount;

    /**
     * 平均文字長度
     */
    private Double averageLength;

    /**
     * 最長文字長度
     */
    private Integer maxLength;

    /**
     * 最短文字長度
     */
    private Integer minLength;

    /**
     * 時間序列數據（日期 -> 文字內容）
     */
    private Map<String, String> timelineData;
}
