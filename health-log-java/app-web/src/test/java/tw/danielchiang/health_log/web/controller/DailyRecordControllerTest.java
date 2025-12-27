package tw.danielchiang.health_log.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import tw.danielchiang.health_log.model.dto.DailyRecordDetailDTO;
import tw.danielchiang.health_log.model.dto.RecordRequestDTO;
import tw.danielchiang.health_log.service.DailyRecordService;
import tw.danielchiang.health_log.web.util.SecurityUtil;

/**
 * DailyRecordController 測試類
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DailyRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DailyRecordService dailyRecordService;

    @MockBean
    private SecurityUtil securityUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testUserId = 1L;
    private LocalDate testDate = LocalDate.of(2024, 1, 15);
    private DailyRecordDetailDTO recordDetail;
    private RecordRequestDTO recordRequest;

    @BeforeEach
    void setUp() {
        // Mock SecurityUtil 返回測試用戶 ID
        try {
            when(securityUtil.getCurrentUserId(any())).thenReturn(testUserId);
        } catch (Exception e) {
            // Ignore
        }

        recordDetail = new DailyRecordDetailDTO();
        recordDetail.setRecordId(1L);
        recordDetail.setRecordDate(testDate);
        recordDetail.setCreatedAt(OffsetDateTime.now());
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("體重", "70.5");
        recordDetail.setFieldValues(fieldValues);

        recordRequest = new RecordRequestDTO();
        recordRequest.setRecordDate(testDate);
        Map<String, String> requestFieldValues = new HashMap<>();
        requestFieldValues.put("體重", "70.5");
        recordRequest.setFieldValues(requestFieldValues);
    }

    @Test
    @WithMockUser
    void testGetAllRecords_Success() throws Exception {
        List<DailyRecordDetailDTO> records = Arrays.asList(recordDetail);
        when(dailyRecordService.getAllRecordsByUserId(testUserId)).thenReturn(records);

        mockMvc.perform(get("/api/records")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordId").value(1))
                .andExpect(jsonPath("$[0].recordDate").value("2024-01-15"))
                .andExpect(jsonPath("$[0].fieldValues.體重").value("70.5"));
    }

    @Test
    @WithMockUser
    void testGetAllRecords_Unauthorized() throws Exception {
        doThrow(new IllegalStateException("無法獲取當前用戶 ID"))
                .when(securityUtil).getCurrentUserId(any());

        mockMvc.perform(get("/api/records")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void testGetRecordByDate_Success() throws Exception {
        when(dailyRecordService.getRecordByDate(testUserId, testDate))
                .thenReturn(Optional.of(recordDetail));

        mockMvc.perform(get("/api/records/2024-01-15")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value(1))
                .andExpect(jsonPath("$.recordDate").value("2024-01-15"))
                .andExpect(jsonPath("$.fieldValues.體重").value("70.5"));
    }

    @Test
    @WithMockUser
    void testGetRecordByDate_NotFound() throws Exception {
        when(dailyRecordService.getRecordByDate(testUserId, testDate))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/records/2024-01-15")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testSaveRecord_Success() throws Exception {
        when(dailyRecordService.saveRecord(eq(testUserId), any(RecordRequestDTO.class)))
                .thenReturn(recordDetail);

        mockMvc.perform(post("/api/records")
                .with(csrf())
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value(1))
                .andExpect(jsonPath("$.recordDate").value("2024-01-15"));
    }

    @Test
    @WithMockUser
    void testSaveRecord_InvalidData() throws Exception {
        when(dailyRecordService.saveRecord(eq(testUserId), any(RecordRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid field values"));

        mockMvc.perform(post("/api/records")
                .with(csrf())
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testDeleteRecord_Success() throws Exception {
        doNothing().when(dailyRecordService).deleteRecord(testUserId, testDate);

        mockMvc.perform(delete("/api/records/2024-01-15")
                .with(csrf())
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void testDeleteRecord_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Record not found"))
                .when(dailyRecordService).deleteRecord(testUserId, testDate);

        mockMvc.perform(delete("/api/records/2024-01-15")
                .with(csrf())
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound());
    }
}

