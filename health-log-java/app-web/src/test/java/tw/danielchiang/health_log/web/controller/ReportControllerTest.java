package tw.danielchiang.health_log.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tw.danielchiang.health_log.model.dto.reponse.TrendDataPointDTO;
import tw.danielchiang.health_log.service.ReportService;
import tw.danielchiang.health_log.web.util.SecurityUtil;

/**
 * ReportController 測試類
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private SecurityUtil securityUtil;

    private Long testUserId = 1L;
    private LocalDate startDate = LocalDate.of(2024, 1, 1);
    private LocalDate endDate = LocalDate.of(2024, 1, 31);
    private String fieldName = "體重";

    @BeforeEach
    void setUp() {
        // Mock SecurityUtil 返回測試用戶 ID
        try {
            when(securityUtil.getCurrentUserId(any())).thenReturn(testUserId);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    @WithMockUser
    void testGetTrendData_Success() throws Exception {
        TrendDataPointDTO point1 = new TrendDataPointDTO(LocalDate.of(2024, 1, 15), "70.5");
        TrendDataPointDTO point2 = new TrendDataPointDTO(LocalDate.of(2024, 1, 16), "71.0");
        List<TrendDataPointDTO> trendData = Arrays.asList(point1, point2);

        when(reportService.getTrendData(eq(testUserId), eq(fieldName), eq(startDate), eq(endDate)))
                .thenReturn(trendData);

        mockMvc.perform(get("/api/reports/trend")
                .param("fieldName", fieldName)
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("includeNulls", "false")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2024-01-15"))
                .andExpect(jsonPath("$[0].value").value("70.5"))
                .andExpect(jsonPath("$[1].date").value("2024-01-16"))
                .andExpect(jsonPath("$[1].value").value("71.0"));
    }

    @Test
    @WithMockUser
    void testGetTrendDataWithNulls_Success() throws Exception {
        TrendDataPointDTO point1 = new TrendDataPointDTO(LocalDate.of(2024, 1, 15), "70.5");
        TrendDataPointDTO point2 = new TrendDataPointDTO(LocalDate.of(2024, 1, 16), null);
        List<TrendDataPointDTO> trendData = Arrays.asList(point1, point2);

        when(reportService.getTrendDataWithNulls(eq(testUserId), eq(fieldName), eq(startDate), eq(endDate)))
                .thenReturn(trendData);

        mockMvc.perform(get("/api/reports/trend")
                .param("fieldName", fieldName)
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .param("includeNulls", "true")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("70.5"))
                .andExpect(jsonPath("$[1].value").isEmpty());
    }

    @Test
    @WithMockUser
    void testGetTrendData_Unauthorized() throws Exception {
        doThrow(new IllegalStateException("無法獲取當前用戶 ID"))
                .when(securityUtil).getCurrentUserId(any());

        mockMvc.perform(get("/api/reports/trend")
                .param("fieldName", fieldName)
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void testGetTrendData_InvalidRequest() throws Exception {
        String invalidFieldName = "不存在的欄位";
        when(reportService.getTrendData(eq(testUserId), eq(invalidFieldName), eq(startDate), eq(endDate)))
                .thenThrow(new IllegalArgumentException("Invalid field name"));

        mockMvc.perform(get("/api/reports/trend")
                .param("fieldName", invalidFieldName)
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testGetTrendData_DefaultIncludeNulls() throws Exception {
        TrendDataPointDTO point1 = new TrendDataPointDTO(LocalDate.of(2024, 1, 15), "70.5");
        List<TrendDataPointDTO> trendData = Arrays.asList(point1);

        when(reportService.getTrendData(eq(testUserId), eq(fieldName), eq(startDate), eq(endDate)))
                .thenReturn(trendData);

        // 不提供 includeNulls 參數，應該默認為 false
        mockMvc.perform(get("/api/reports/trend")
                .param("fieldName", fieldName)
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].value").value("70.5"));
    }
}

