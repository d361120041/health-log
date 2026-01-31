package tw.danielchiang.health_log.model.dto.reponse;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NUMBER 類型報表 DTO
 * 包含趨勢數據和統計摘要
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NumberReportDTO {

    /**
     * 趨勢數據點列表
     */
    private List<TrendDataPointDTO> trendData;

    /**
     * 統計摘要
     */
    private NumberStatisticsDTO statistics;
}
