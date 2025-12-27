package tw.danielchiang.health_log.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import tw.danielchiang.health_log.data.repository.UserRepository;
import tw.danielchiang.health_log.model.dto.DailyRecordDetailDTO;
import tw.danielchiang.health_log.model.dto.RecordRequestDTO;
import tw.danielchiang.health_log.model.entity.DailyRecord;
import tw.danielchiang.health_log.model.entity.FieldSetting;
import tw.danielchiang.health_log.model.entity.RecordData;
import tw.danielchiang.health_log.model.entity.Role;
import tw.danielchiang.health_log.model.entity.User;

/**
 * DailyRecordService 測試
 */
@ExtendWith(MockitoExtension.class)
class DailyRecordServiceTest {

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private RecordDataRepository recordDataRepository;

    @Mock
    private FieldSettingRepository fieldSettingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DailyRecordService dailyRecordService;

    private User testUser;
    private Role testRole;
    private FieldSetting testFieldSetting;
    private DailyRecord testDailyRecord;
    private RecordData testRecordData;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 1);

        // 建立測試角色
        testRole = new Role();
        testRole.setRoleId(1);
        testRole.setRoleName("USER");

        // 建立測試使用者
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole(testRole);
        testUser.setIsActive(true);

        // 建立測試欄位設定
        testFieldSetting = new FieldSetting();
        testFieldSetting.setSettingId(1);
        testFieldSetting.setFieldName("體重");
        testFieldSetting.setDataType("NUMBER");
        testFieldSetting.setUnit("kg");
        testFieldSetting.setIsRequired(false);
        testFieldSetting.setIsActive(true);

        // 建立測試記錄
        testDailyRecord = new DailyRecord();
        testDailyRecord.setRecordId(1L);
        testDailyRecord.setUser(testUser);
        testDailyRecord.setRecordDate(testDate);
        testDailyRecord.setCreatedAt(OffsetDateTime.now());
        testDailyRecord.setRecordDataList(new ArrayList<>());

        // 建立測試記錄數據
        testRecordData = new RecordData();
        testRecordData.setDataId(1L);
        testRecordData.setDailyRecord(testDailyRecord);
        testRecordData.setFieldSetting(testFieldSetting);
        testRecordData.setValueText("70");
    }

    @Test
    void testSaveRecord_CreateNewRecord() {
        // Given
        RecordRequestDTO request = new RecordRequestDTO();
        request.setRecordDate(testDate);
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("體重", "70");
        request.setFieldValues(fieldValues);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(dailyRecordRepository.findByUserIdAndRecordDate(1L, testDate)).thenReturn(Optional.empty());
        when(dailyRecordRepository.save(any(DailyRecord.class))).thenAnswer(invocation -> {
            DailyRecord record = invocation.getArgument(0);
            record.setRecordId(1L);
            record.setCreatedAt(OffsetDateTime.now());
            return record;
        });
        when(fieldSettingRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(testFieldSetting));
        when(recordDataRepository.findByDailyRecordRecordId(1L)).thenReturn(new ArrayList<>());
        when(recordDataRepository.save(any(RecordData.class))).thenReturn(testRecordData);
        when(dailyRecordRepository.findById(1L)).thenReturn(Optional.of(testDailyRecord));

        // When
        DailyRecordDetailDTO result = dailyRecordService.saveRecord(1L, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecordDate()).isEqualTo(testDate);
        verify(userRepository, times(1)).findById(1L);
        verify(dailyRecordRepository, times(1)).findByUserIdAndRecordDate(1L, testDate);
        verify(dailyRecordRepository, times(1)).save(any(DailyRecord.class));
        verify(fieldSettingRepository, times(1)).findByIsActiveTrue();
        verify(recordDataRepository, times(1)).save(any(RecordData.class));
    }

    @Test
    void testSaveRecord_UpdateExistingRecord() {
        // Given
        RecordRequestDTO request = new RecordRequestDTO();
        request.setRecordDate(testDate);
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("體重", "75");
        request.setFieldValues(fieldValues);

        List<RecordData> existingData = Arrays.asList(testRecordData);
        testDailyRecord.setRecordDataList(existingData);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(dailyRecordRepository.findByUserIdAndRecordDate(1L, testDate)).thenReturn(Optional.of(testDailyRecord));
        when(fieldSettingRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(testFieldSetting));
        when(recordDataRepository.findByDailyRecordRecordId(1L)).thenReturn(existingData);
        when(recordDataRepository.save(any(RecordData.class))).thenReturn(testRecordData);
        when(dailyRecordRepository.findById(1L)).thenReturn(Optional.of(testDailyRecord));

        // When
        DailyRecordDetailDTO result = dailyRecordService.saveRecord(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(recordDataRepository, times(1)).deleteAll(existingData);
        verify(recordDataRepository, times(1)).save(any(RecordData.class));
    }

    @Test
    void testSaveRecord_WhenUserNotFound() {
        // Given
        RecordRequestDTO request = new RecordRequestDTO();
        request.setRecordDate(testDate);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> dailyRecordService.saveRecord(999L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("使用者不存在");

        verify(userRepository, times(1)).findById(999L);
        verify(dailyRecordRepository, never()).findByUserIdAndRecordDate(any(), any());
    }

    @Test
    void testSaveRecord_WhenRequiredFieldIsEmpty() {
        // Given
        FieldSetting requiredField = new FieldSetting();
        requiredField.setSettingId(2);
        requiredField.setFieldName("必填欄位");
        requiredField.setDataType("TEXT");
        requiredField.setIsRequired(true);
        requiredField.setIsActive(true);

        RecordRequestDTO request = new RecordRequestDTO();
        request.setRecordDate(testDate);
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("必填欄位", "");
        request.setFieldValues(fieldValues);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(dailyRecordRepository.findByUserIdAndRecordDate(1L, testDate)).thenReturn(Optional.empty());
        when(dailyRecordRepository.save(any(DailyRecord.class))).thenAnswer(invocation -> {
            DailyRecord record = invocation.getArgument(0);
            record.setRecordId(1L);
            return record;
        });
        when(fieldSettingRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(requiredField));
        when(recordDataRepository.findByDailyRecordRecordId(1L)).thenReturn(new ArrayList<>());

        // When & Then
        assertThatThrownBy(() -> dailyRecordService.saveRecord(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("必填欄位不能為空");

        verify(recordDataRepository, never()).save(any(RecordData.class));
    }

    @Test
    void testGetRecordByDate_WhenExists() {
        // Given
        testDailyRecord.setRecordDataList(Arrays.asList(testRecordData));
        when(dailyRecordRepository.findByUserIdAndRecordDate(1L, testDate))
            .thenReturn(Optional.of(testDailyRecord));

        // When
        Optional<DailyRecordDetailDTO> result = dailyRecordService.getRecordByDate(1L, testDate);

        // Then
        assertThat(result).isPresent();
        DailyRecordDetailDTO dto = result.get();
        assertThat(dto.getRecordDate()).isEqualTo(testDate);
        assertThat(dto.getFieldValues()).containsKey("體重");
        assertThat(dto.getFieldValues().get("體重")).isEqualTo("70");
        verify(dailyRecordRepository, times(1)).findByUserIdAndRecordDate(1L, testDate);
    }

    @Test
    void testGetRecordByDate_WhenNotExists() {
        // Given
        when(dailyRecordRepository.findByUserIdAndRecordDate(1L, testDate))
            .thenReturn(Optional.empty());

        // When
        Optional<DailyRecordDetailDTO> result = dailyRecordService.getRecordByDate(1L, testDate);

        // Then
        assertThat(result).isEmpty();
        verify(dailyRecordRepository, times(1)).findByUserIdAndRecordDate(1L, testDate);
    }

    @Test
    void testGetAllRecordsByUserId() {
        // Given
        DailyRecord record1 = new DailyRecord();
        record1.setRecordId(1L);
        record1.setRecordDate(LocalDate.of(2024, 1, 2));
        record1.setRecordDataList(Arrays.asList(testRecordData));

        DailyRecord record2 = new DailyRecord();
        record2.setRecordId(2L);
        record2.setRecordDate(LocalDate.of(2024, 1, 1));
        record2.setRecordDataList(new ArrayList<>());

        when(dailyRecordRepository.findByUserIdOrderByRecordDateDesc(1L))
            .thenReturn(Arrays.asList(record1, record2));

        // When
        List<DailyRecordDetailDTO> result = dailyRecordService.getAllRecordsByUserId(1L);

        // Then
        assertThat(result).hasSize(2);
        verify(dailyRecordRepository, times(1)).findByUserIdOrderByRecordDateDesc(1L);
    }

    @Test
    void testDeleteRecord() {
        // Given
        when(dailyRecordRepository.findByUserIdAndRecordDate(1L, testDate))
            .thenReturn(Optional.of(testDailyRecord));

        // When
        dailyRecordService.deleteRecord(1L, testDate);

        // Then
        verify(dailyRecordRepository, times(1)).findByUserIdAndRecordDate(1L, testDate);
        verify(dailyRecordRepository, times(1)).delete(testDailyRecord);
    }

    @Test
    void testDeleteRecord_WhenNotExists() {
        // Given
        when(dailyRecordRepository.findByUserIdAndRecordDate(1L, testDate))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> dailyRecordService.deleteRecord(1L, testDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("記錄不存在");

        verify(dailyRecordRepository, times(1)).findByUserIdAndRecordDate(1L, testDate);
        verify(dailyRecordRepository, never()).delete(any(DailyRecord.class));
    }
}

