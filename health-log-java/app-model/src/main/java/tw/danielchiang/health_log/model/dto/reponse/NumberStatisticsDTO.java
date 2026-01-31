package tw.danielchiang.health_log.model.dto.reponse;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NUMBER 類型統計摘要 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NumberStatisticsDTO {

    /**
     * 平均值
     */
    private BigDecimal average;

    /**
     * 最大值
     */
    private BigDecimal max;

    /**
     * 最小值
     */
    private BigDecimal min;

    /**
     * 總和
     */
    private BigDecimal sum;

    /**
     * 記錄數量
     */
    private Long count;

    /**
     * 標準差
     */
    private BigDecimal standardDeviation;

    /**
     * 中位數
     */
    private BigDecimal median;
}
