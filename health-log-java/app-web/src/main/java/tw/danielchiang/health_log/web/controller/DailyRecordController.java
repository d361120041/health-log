package tw.danielchiang.health_log.web.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.danielchiang.health_log.model.domain.PageableData;
import tw.danielchiang.health_log.model.dto.reponse.DailyRecordDetailDTO;
import tw.danielchiang.health_log.model.dto.reponse.ResponseDTO;
import tw.danielchiang.health_log.model.dto.request.RecordRequestDTO;
import tw.danielchiang.health_log.model.dto.request.SearchRequestDTO;
import tw.danielchiang.health_log.model.entity.DailyRecord;
import tw.danielchiang.health_log.service.DailyRecordService;
import tw.danielchiang.health_log.web.util.SecurityUtil;

/**
 * 每日記錄控制器
 * 處理每日記錄的 CRUD 請求
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Slf4j
public class DailyRecordController {

    private final DailyRecordService dailyRecordService;
    private final SecurityUtil securityUtil;

    /**
     * 獲取當前用戶的所有記錄
     * GET /api/records
     */
    @Deprecated
    @GetMapping
    public ResponseEntity<List<DailyRecordDetailDTO>> getAllRecords(HttpServletRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId(request);
            List<DailyRecordDetailDTO> records = dailyRecordService.getAllRecordsByUserId(userId);
            return ResponseEntity.ok(records);
        } catch (IllegalStateException e) {
            log.warn("Failed to get records: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/search")
    public ResponseEntity<ResponseDTO<DailyRecordDetailDTO>> getRecordsByUserId(
            @Valid @RequestBody SearchRequestDTO<DailyRecord> request,
            HttpServletRequest httpRequest) {
        ResponseDTO<DailyRecordDetailDTO> responseDTO = new ResponseDTO<>();
        try {
            Long userId = securityUtil.getCurrentUserId(httpRequest);
            Page<DailyRecordDetailDTO> records = dailyRecordService.getRecordsByUserId(userId, request);
            responseDTO.setData(PageableData.of(records));
            return ResponseEntity.ok().body(responseDTO);
        } catch (IllegalStateException e) {
            log.warn("Failed to get records: {}", e.getMessage());
            responseDTO.setStatus(HttpStatus.UNAUTHORIZED.value());
            responseDTO.setMessage(HttpStatus.UNAUTHORIZED.name());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDTO);
        }
    }

    /**
     * 根據日期獲取單日記錄
     * GET /api/records/{date}
     */
    @GetMapping("/{date}")
    public ResponseEntity<DailyRecordDetailDTO> getRecordByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId(request);
            return dailyRecordService.getRecordByDate(userId, date)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            log.warn("Failed to get record: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 創建或更新每日記錄
     * POST /api/records
     */
    @PostMapping
    public ResponseEntity<DailyRecordDetailDTO> saveRecord(
            @Valid @RequestBody RecordRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = securityUtil.getCurrentUserId(httpRequest);
            DailyRecordDetailDTO saved = dailyRecordService.saveRecord(userId, request);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            log.warn("Failed to save record: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid record data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 刪除指定日期的記錄
     * DELETE /api/records/{date}
     */
    @DeleteMapping("/{date}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId(request);
            dailyRecordService.deleteRecord(userId, date);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Failed to delete record: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            log.warn("Record not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
