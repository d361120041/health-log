package tw.danielchiang.health_log.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.danielchiang.health_log.model.entity.FieldSetting;
import tw.danielchiang.health_log.service.FieldSettingService;

/**
 * 欄位設定控制器
 * 處理欄位設定的 CRUD 請求
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class FieldSettingController {

    private final FieldSettingService fieldSettingService;

    /**
     * 獲取所有啟用的欄位設定（公開端點，用於動態表單渲染）
     * GET /api/settings/fields
     */
    @GetMapping("/settings/fields")
    public ResponseEntity<List<FieldSetting>> getActiveFieldSettings() {
        List<FieldSetting> fieldSettings = fieldSettingService.getAllActiveFieldSettings();
        return ResponseEntity.ok(fieldSettings);
    }

    /**
     * 獲取所有欄位設定（包含未啟用的，僅 Admin）
     * GET /api/admin/settings/fields
     */
    @GetMapping("/admin/settings/fields")
    public ResponseEntity<List<FieldSetting>> getAllFieldSettings() {
        List<FieldSetting> fieldSettings = fieldSettingService.getAllFieldSettings();
        return ResponseEntity.ok(fieldSettings);
    }

    /**
     * 根據 ID 獲取欄位設定（僅 Admin）
     * GET /api/admin/settings/fields/{id}
     */
    @GetMapping("/admin/settings/fields/{id}")
    public ResponseEntity<FieldSetting> getFieldSettingById(@PathVariable Integer id) {
        return fieldSettingService.getFieldSettingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 創建欄位設定（僅 Admin）
     * POST /api/admin/settings/fields
     */
    @PostMapping("/admin/settings/fields")
    public ResponseEntity<FieldSetting> createFieldSetting(@Valid @RequestBody FieldSetting fieldSetting) {
        try {
            FieldSetting created = fieldSettingService.createFieldSetting(fieldSetting);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create field setting: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新欄位設定（僅 Admin）
     * PUT /api/admin/settings/fields/{id}
     */
    @PutMapping("/admin/settings/fields/{id}")
    public ResponseEntity<FieldSetting> updateFieldSetting(
            @PathVariable Integer id,
            @Valid @RequestBody FieldSetting fieldSetting) {
        try {
            FieldSetting updated = fieldSettingService.updateFieldSetting(id, fieldSetting);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update field setting: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 刪除欄位設定（軟刪除，僅 Admin）
     * DELETE /api/admin/settings/fields/{id}
     */
    @DeleteMapping("/admin/settings/fields/{id}")
    public ResponseEntity<Void> deleteFieldSetting(@PathVariable Integer id) {
        try {
            fieldSettingService.deleteFieldSetting(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete field setting: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}

