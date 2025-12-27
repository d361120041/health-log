package tw.danielchiang.health_log.service;

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

import tw.danielchiang.health_log.data.repository.FieldSettingRepository;
import tw.danielchiang.health_log.model.entity.FieldSetting;

/**
 * FieldSettingService 測試
 */
@ExtendWith(MockitoExtension.class)
class FieldSettingServiceTest {

    @Mock
    private FieldSettingRepository fieldSettingRepository;

    @InjectMocks
    private FieldSettingService fieldSettingService;

    private FieldSetting testFieldSetting;

    @BeforeEach
    void setUp() {
        testFieldSetting = new FieldSetting();
        testFieldSetting.setSettingId(1);
        testFieldSetting.setFieldName("體重");
        testFieldSetting.setDataType("NUMBER");
        testFieldSetting.setUnit("kg");
        testFieldSetting.setIsRequired(false);
        testFieldSetting.setIsActive(true);
    }

    @Test
    void testGetAllActiveFieldSettings() {
        // Given
        FieldSetting active1 = new FieldSetting();
        active1.setSettingId(1);
        active1.setFieldName("體重");
        active1.setIsActive(true);
        
        FieldSetting active2 = new FieldSetting();
        active2.setSettingId(2);
        active2.setFieldName("身高");
        active2.setIsActive(true);
        
        List<FieldSetting> activeSettings = Arrays.asList(active1, active2);
        when(fieldSettingRepository.findByIsActiveTrue()).thenReturn(activeSettings);

        // When
        List<FieldSetting> result = fieldSettingService.getAllActiveFieldSettings();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(active1, active2);
        verify(fieldSettingRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void testGetAllFieldSettings() {
        // Given
        List<FieldSetting> allSettings = Arrays.asList(testFieldSetting);
        when(fieldSettingRepository.findAll()).thenReturn(allSettings);

        // When
        List<FieldSetting> result = fieldSettingService.getAllFieldSettings();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testFieldSetting);
        verify(fieldSettingRepository, times(1)).findAll();
    }

    @Test
    void testGetFieldSettingById_WhenExists() {
        // Given
        Integer settingId = 1;
        when(fieldSettingRepository.findById(settingId)).thenReturn(Optional.of(testFieldSetting));

        // When
        Optional<FieldSetting> result = fieldSettingService.getFieldSettingById(settingId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testFieldSetting);
        verify(fieldSettingRepository, times(1)).findById(settingId);
    }

    @Test
    void testGetFieldSettingById_WhenNotExists() {
        // Given
        Integer settingId = 999;
        when(fieldSettingRepository.findById(settingId)).thenReturn(Optional.empty());

        // When
        Optional<FieldSetting> result = fieldSettingService.getFieldSettingById(settingId);

        // Then
        assertThat(result).isEmpty();
        verify(fieldSettingRepository, times(1)).findById(settingId);
    }

    @Test
    void testGetFieldSettingByFieldName() {
        // Given
        String fieldName = "體重";
        when(fieldSettingRepository.findByFieldName(fieldName)).thenReturn(Optional.of(testFieldSetting));

        // When
        Optional<FieldSetting> result = fieldSettingService.getFieldSettingByFieldName(fieldName);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testFieldSetting);
        verify(fieldSettingRepository, times(1)).findByFieldName(fieldName);
    }

    @Test
    void testCreateFieldSetting_Success() {
        // Given
        FieldSetting newFieldSetting = new FieldSetting();
        newFieldSetting.setFieldName("新欄位");
        newFieldSetting.setDataType("TEXT");
        newFieldSetting.setIsRequired(false);
        newFieldSetting.setIsActive(true);

        when(fieldSettingRepository.existsByFieldName(newFieldSetting.getFieldName())).thenReturn(false);
        when(fieldSettingRepository.save(any(FieldSetting.class))).thenReturn(newFieldSetting);

        // When
        FieldSetting result = fieldSettingService.createFieldSetting(newFieldSetting);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFieldName()).isEqualTo("新欄位");
        verify(fieldSettingRepository, times(1)).existsByFieldName(newFieldSetting.getFieldName());
        verify(fieldSettingRepository, times(1)).save(newFieldSetting);
    }

    @Test
    void testCreateFieldSetting_WhenFieldNameExists() {
        // Given
        FieldSetting newFieldSetting = new FieldSetting();
        newFieldSetting.setFieldName("體重");
        
        when(fieldSettingRepository.existsByFieldName(newFieldSetting.getFieldName())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> fieldSettingService.createFieldSetting(newFieldSetting))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("欄位名稱已存在");
        
        verify(fieldSettingRepository, times(1)).existsByFieldName(newFieldSetting.getFieldName());
        verify(fieldSettingRepository, never()).save(any(FieldSetting.class));
    }

    @Test
    void testUpdateFieldSetting_Success() {
        // Given
        Integer settingId = 1;
        FieldSetting updatedData = new FieldSetting();
        updatedData.setFieldName("體重（更新）");
        updatedData.setDataType("NUMBER");
        updatedData.setUnit("kg");
        updatedData.setIsRequired(true);
        updatedData.setIsActive(true);

        when(fieldSettingRepository.findById(settingId)).thenReturn(Optional.of(testFieldSetting));
        when(fieldSettingRepository.existsByFieldName(updatedData.getFieldName())).thenReturn(false);
        when(fieldSettingRepository.save(any(FieldSetting.class))).thenReturn(testFieldSetting);

        // When
        FieldSetting result = fieldSettingService.updateFieldSetting(settingId, updatedData);

        // Then
        assertThat(result).isNotNull();
        verify(fieldSettingRepository, times(1)).findById(settingId);
        verify(fieldSettingRepository, times(1)).save(any(FieldSetting.class));
    }

    @Test
    void testUpdateFieldSetting_WhenNotExists() {
        // Given
        Integer settingId = 999;
        FieldSetting updatedData = new FieldSetting();
        updatedData.setFieldName("新名稱");
        
        when(fieldSettingRepository.findById(settingId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fieldSettingService.updateFieldSetting(settingId, updatedData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("欄位設定不存在");
        
        verify(fieldSettingRepository, times(1)).findById(settingId);
        verify(fieldSettingRepository, never()).save(any(FieldSetting.class));
    }

    @Test
    void testDeleteFieldSetting() {
        // Given
        Integer settingId = 1;
        when(fieldSettingRepository.findById(settingId)).thenReturn(Optional.of(testFieldSetting));
        when(fieldSettingRepository.save(any(FieldSetting.class))).thenReturn(testFieldSetting);

        // When
        fieldSettingService.deleteFieldSetting(settingId);

        // Then
        assertThat(testFieldSetting.getIsActive()).isFalse();
        verify(fieldSettingRepository, times(1)).findById(settingId);
        verify(fieldSettingRepository, times(1)).save(testFieldSetting);
    }

    @Test
    void testDeleteFieldSetting_WhenNotExists() {
        // Given
        Integer settingId = 999;
        when(fieldSettingRepository.findById(settingId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fieldSettingService.deleteFieldSetting(settingId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("欄位設定不存在");
        
        verify(fieldSettingRepository, times(1)).findById(settingId);
        verify(fieldSettingRepository, never()).save(any(FieldSetting.class));
    }

    @Test
    void testHardDeleteFieldSetting() {
        // Given
        Integer settingId = 1;
        when(fieldSettingRepository.existsById(settingId)).thenReturn(true);

        // When
        fieldSettingService.hardDeleteFieldSetting(settingId);

        // Then
        verify(fieldSettingRepository, times(1)).existsById(settingId);
        verify(fieldSettingRepository, times(1)).deleteById(settingId);
    }

    @Test
    void testHardDeleteFieldSetting_WhenNotExists() {
        // Given
        Integer settingId = 999;
        when(fieldSettingRepository.existsById(settingId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> fieldSettingService.hardDeleteFieldSetting(settingId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("欄位設定不存在");
        
        verify(fieldSettingRepository, times(1)).existsById(settingId);
        verify(fieldSettingRepository, never()).deleteById(any(Integer.class));
    }
}

