package tw.danielchiang.health_log.web.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.danielchiang.health_log.model.dto.reponse.EnumDistributionDTO;
import tw.danielchiang.health_log.model.dto.reponse.EnumTrendDTO;
import tw.danielchiang.health_log.model.dto.reponse.NumberReportDTO;
import tw.danielchiang.health_log.model.dto.reponse.TextAnalysisDTO;
import tw.danielchiang.health_log.model.dto.reponse.TrendDataPointDTO;
import tw.danielchiang.health_log.service.ReportService;
import tw.danielchiang.health_log.web.util.SecurityUtil;

/**
 * 報告控制器
 * 處理數據報告相關請求
 * 支援 NUMBER, ENUM, TEXT 三種類型的報表
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtil securityUtil;

    // ==================== NUMBER 類型報表 ====================

    /**
     * 獲取 NUMBER 類型欄位的完整報表（包含趨勢和統計）
     * GET /api/reports/number?fieldName={fieldName}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/number")
    public ResponseEntity<NumberReportDTO> getNumberReport(
            @RequestParam String fieldName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId(request);
            NumberReportDTO report = reportService.getNumberReport(userId, fieldName, startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (IllegalStateException e) {
            log.warn("Failed to get number report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid number report request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 獲取趨勢數據（NUMBER 類型，向後兼容）
     * GET /api/reports/trend?fieldName={fieldName}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/trend")
    public ResponseEntity<List<TrendDataPointDTO>> getTrendData(
            @RequestParam String fieldName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") boolean includeNulls,
            HttpServletRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId(request);
            
            List<TrendDataPointDTO> trendData;
            if (includeNulls) {
                trendData = reportService.getTrendDataWithNulls(userId, fieldName, startDate, endDate);
            } else {
                trendData = reportService.getTrendData(userId, fieldName, startDate, endDate);
            }
            
            return ResponseEntity.ok(trendData);
        } catch (IllegalStateException e) {
            log.warn("Failed to get trend data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trend data request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== ENUM 類型報表 ====================

    /**
     * 獲取 ENUM 類型欄位的分佈統計
     * GET /api/reports/enum/distribution?fieldName={fieldName}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/enum/distribution")
    public ResponseEntity<EnumDistributionDTO> getEnumDistribution(
            @RequestParam String fieldName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId(request);
            EnumDistributionDTO distribution = reportService.getEnumDistribution(userId, fieldName, startDate, endDate);
            return ResponseEntity.ok(distribution);
        } catch (IllegalStateException e) {
            log.warn("Failed to get enum distribution: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum distribution request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 獲取 ENUM 類型欄位的時間序列趨勢
     * GET /api/reports/enum/trend?fieldName={fieldName}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/enum/trend")
    public ResponseEntity<EnumTrendDTO> getEnumTrend(
            @RequestParam String fieldName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId(request);
            EnumTrendDTO trend = reportService.getEnumTrend(userId, fieldName, startDate, endDate);
            return ResponseEntity.ok(trend);
        } catch (IllegalStateException e) {
            log.warn("Failed to get enum trend: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum trend request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== TEXT 類型報表 ====================

    /**
     * 獲取 TEXT 類型欄位的文字分析報表
     * GET /api/reports/text/analysis?fieldName={fieldName}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/text/analysis")
    public ResponseEntity<TextAnalysisDTO> getTextAnalysis(
            @RequestParam String fieldName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId(request);
            TextAnalysisDTO analysis = reportService.getTextAnalysis(userId, fieldName, startDate, endDate);
            return ResponseEntity.ok(analysis);
        } catch (IllegalStateException e) {
            log.warn("Failed to get text analysis: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid text analysis request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

