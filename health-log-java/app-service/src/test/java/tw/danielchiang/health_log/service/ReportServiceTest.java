package tw.danielchiang.health_log.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import tw.danielchiang.health_log.data.repository.DailyRecordRepository;
import tw.danielchiang.health_log.data.repository.FieldSettingRepository;
import tw.danielchiang.health_log.data.repository.RecordDataRepository;
import tw.danielchiang.health_log.model.dto.TrendDataPointDTO;
import tw.danielchiang.health_log.model.entity.DailyRecord;
import tw.danielchiang.health_log.model.entity.FieldSetting;
import tw.danielchiang.health_log.model.entity.RecordData;
import tw.danielchiang.health_log.model.entity.Role;
import tw.danielchiang.health_log.model.entity.User;

/**
 * ReportService 測試
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private RecordDataRepository recordDataRepository;

    @Mock
    private FieldSettingRepository fieldSettingRepository;

    @InjectMocks
    private ReportService reportService;

    private FieldSetting testFieldSetting;
    private DailyRecord testDailyRecord1;
    private DailyRecord testDailyRecord2;
    private RecordData testRecordData1;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);

        // 建立測試欄位設定
        testFieldSetting = new FieldSetting();
        testFieldSetting.setSettingId(1);
        testFieldSetting.setFieldName("體重");
        testFieldSetting.setDataType("NUMBER");
        testFieldSetting.setUnit("kg");
        testFieldSetting.setIsActive(true);

        // 建立測試使用者
        User testUser = new User();
        testUser.setId(1L);
        Role role = new Role();
        role.setRoleId(1);
        role.setRoleName("USER");
        testUser.setRole(role);

        // 建立測試記錄
        testDailyRecord1 = new DailyRecord();
        testDailyRecord1.setRecordId(1L);
        testDailyRecord1.setUser(testUser);
        testDailyRecord1.setRecordDate(LocalDate.of(2024, 1, 5));

        testDailyRecord2 = new DailyRecord();
        testDailyRecord2.setRecordId(2L);
        testDailyRecord2.setUser(testUser);
        testDailyRecord2.setRecordDate(LocalDate.of(2024, 1, 10));

        // 建立測試記錄數據
        testRecordData1 = new RecordData();
        testRecordData1.setDataId(1L);
        testRecordData1.setDailyRecord(testDailyRecord1);
        testRecordData1.setFieldSetting(testFieldSetting);
        testRecordData1.setValueText("70");
    }

    @Test
    void testGetTrendData_Success() {
        // Given
        List<DailyRecord> allRecords = Arrays.asList(testDailyRecord2, testDailyRecord1);
        
        when(fieldSettingRepository.findByFieldName("體重")).thenReturn(Optional.of(testFieldSetting));
        when(dailyRecordRepository.findByUserIdOrderByRecordDateDesc(1L)).thenReturn(allRecords);
        when(recordDataRepository.findBySettingIdAndRecordId(1, 1L))
            .thenReturn(Optional.of(testRecordData1));
        when(recordDataRepository.findBySettingIdAndRecordId(1, 2L))
            .thenReturn(Optional.empty());

        // When
        List<TrendDataPointDTO> result = reportService.getTrendData(1L, "體重", startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2024, 1, 5));
        assertThat(result.get(0).getValue()).isEqualTo("70");
        
        verify(fieldSettingRepository, times(1)).findByFieldName("體重");
        verify(dailyRecordRepository, times(1)).findByUserIdOrderByRecordDateDesc(1L);
    }

    @Test
    void testGetTrendData_WhenFieldSettingNotFound() {
        // Given
        when(fieldSettingRepository.findByFieldName("不存在的欄位")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reportService.getTrendData(1L, "不存在的欄位", startDate, endDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("欄位設定不存在");

        verify(fieldSettingRepository, times(1)).findByFieldName("不存在的欄位");
        verify(dailyRecordRepository, never()).findByUserIdOrderByRecordDateDesc(any());
    }

    @Test
    void testGetTrendData_FiltersByDateRange() {
        // Given
        DailyRecord recordBefore = new DailyRecord();
        recordBefore.setRecordId(3L);
        recordBefore.setRecordDate(LocalDate.of(2023, 12, 31));

        DailyRecord recordAfter = new DailyRecord();
        recordAfter.setRecordId(4L);
        recordAfter.setRecordDate(LocalDate.of(2024, 2, 1));

        List<DailyRecord> allRecords = Arrays.asList(recordAfter, testDailyRecord2, testDailyRecord1, recordBefore);

        when(fieldSettingRepository.findByFieldName("體重")).thenReturn(Optional.of(testFieldSetting));
        when(dailyRecordRepository.findByUserIdOrderByRecordDateDesc(1L)).thenReturn(allRecords);
        when(recordDataRepository.findBySettingIdAndRecordId(1, 1L))
            .thenReturn(Optional.of(testRecordData1));
        when(recordDataRepository.findBySettingIdAndRecordId(1, 2L))
            .thenReturn(Optional.empty());

        // When
        List<TrendDataPointDTO> result = reportService.getTrendData(1L, "體重", startDate, endDate);

        // Then
        // 應該只包含日期範圍內的記錄
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2024, 1, 5));
    }

    @Test
    void testGetTrendDataWithNulls_Success() {
        // Given
        List<DailyRecord> allRecords = Arrays.asList(testDailyRecord2, testDailyRecord1);
        
        when(fieldSettingRepository.findByFieldName("體重")).thenReturn(Optional.of(testFieldSetting));
        when(dailyRecordRepository.findByUserIdOrderByRecordDateDesc(1L)).thenReturn(allRecords);
        when(recordDataRepository.findBySettingIdAndRecordId(1, 1L))
            .thenReturn(Optional.of(testRecordData1));
        when(recordDataRepository.findBySettingIdAndRecordId(1, 2L))
            .thenReturn(Optional.empty());

        // When
        List<TrendDataPointDTO> result = reportService.getTrendDataWithNulls(1L, "體重", startDate, endDate);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2024, 1, 5));
        assertThat(result.get(0).getValue()).isEqualTo("70");
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(result.get(1).getValue()).isEqualTo(""); // 空值
        
        verify(fieldSettingRepository, times(1)).findByFieldName("體重");
        verify(dailyRecordRepository, times(1)).findByUserIdOrderByRecordDateDesc(1L);
    }

    @Test
    void testGetTrendDataWithNulls_WhenFieldSettingNotFound() {
        // Given
        when(fieldSettingRepository.findByFieldName("不存在的欄位")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> reportService.getTrendDataWithNulls(1L, "不存在的欄位", startDate, endDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("欄位設定不存在");

        verify(fieldSettingRepository, times(1)).findByFieldName("不存在的欄位");
        verify(dailyRecordRepository, never()).findByUserIdOrderByRecordDateDesc(any());
    }
}

