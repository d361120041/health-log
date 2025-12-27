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
import tw.danielchiang.health_log.model.dto.TrendDataPointDTO;
import tw.danielchiang.health_log.service.ReportService;
import tw.danielchiang.health_log.web.util.SecurityUtil;

/**
 * 報告控制器
 * 處理數據報告相關請求
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtil securityUtil;

    /**
     * 獲取趨勢數據
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
}

