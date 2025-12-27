package tw.danielchiang.health_log.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
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

import tw.danielchiang.health_log.model.entity.FieldSetting;
import tw.danielchiang.health_log.service.FieldSettingService;

/**
 * FieldSettingController 測試類
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FieldSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FieldSettingService fieldSettingService;

    @Autowired
    private ObjectMapper objectMapper;

    private FieldSetting fieldSetting;

    @BeforeEach
    void setUp() {
        fieldSetting = new FieldSetting();
        fieldSetting.setSettingId(1);
        fieldSetting.setFieldName("體重");
        fieldSetting.setDataType("NUMBER");
        fieldSetting.setUnit("kg");
        fieldSetting.setIsActive(true);
    }

    @Test
    void testGetActiveFieldSettings_Success() throws Exception {
        List<FieldSetting> fieldSettings = Arrays.asList(fieldSetting);
        when(fieldSettingService.getAllActiveFieldSettings()).thenReturn(fieldSettings);

        mockMvc.perform(get("/api/settings/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].settingId").value(1))
                .andExpect(jsonPath("$[0].fieldName").value("體重"))
                .andExpect(jsonPath("$[0].dataType").value("NUMBER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllFieldSettings_Success() throws Exception {
        List<FieldSetting> fieldSettings = Arrays.asList(fieldSetting);
        when(fieldSettingService.getAllFieldSettings()).thenReturn(fieldSettings);

        mockMvc.perform(get("/api/admin/settings/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].settingId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetFieldSettingById_Success() throws Exception {
        when(fieldSettingService.getFieldSettingById(1)).thenReturn(Optional.of(fieldSetting));

        mockMvc.perform(get("/api/admin/settings/fields/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settingId").value(1))
                .andExpect(jsonPath("$.fieldName").value("體重"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetFieldSettingById_NotFound() throws Exception {
        when(fieldSettingService.getFieldSettingById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/settings/fields/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateFieldSetting_Success() throws Exception {
        FieldSetting newFieldSetting = new FieldSetting();
        newFieldSetting.setFieldName("身高");
        newFieldSetting.setDataType("NUMBER");
        newFieldSetting.setUnit("cm");
        newFieldSetting.setIsActive(true);

        when(fieldSettingService.createFieldSetting(any(FieldSetting.class))).thenReturn(fieldSetting);

        mockMvc.perform(post("/api/admin/settings/fields")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newFieldSetting)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.settingId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateFieldSetting_InvalidData() throws Exception {
        FieldSetting invalidFieldSetting = new FieldSetting();

        when(fieldSettingService.createFieldSetting(any(FieldSetting.class)))
                .thenThrow(new IllegalArgumentException("Invalid field setting"));

        mockMvc.perform(post("/api/admin/settings/fields")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidFieldSetting)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateFieldSetting_Success() throws Exception {
        fieldSetting.setFieldName("更新後的體重");

        when(fieldSettingService.updateFieldSetting(anyInt(), any(FieldSetting.class))).thenReturn(fieldSetting);

        mockMvc.perform(put("/api/admin/settings/fields/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fieldSetting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldName").value("更新後的體重"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateFieldSetting_NotFound() throws Exception {
        when(fieldSettingService.updateFieldSetting(anyInt(), any(FieldSetting.class)))
                .thenThrow(new IllegalArgumentException("Field setting not found"));

        mockMvc.perform(put("/api/admin/settings/fields/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fieldSetting)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteFieldSetting_Success() throws Exception {
        doNothing().when(fieldSettingService).deleteFieldSetting(1);

        mockMvc.perform(delete("/api/admin/settings/fields/1")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteFieldSetting_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Field setting not found"))
                .when(fieldSettingService).deleteFieldSetting(999);

        mockMvc.perform(delete("/api/admin/settings/fields/999")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}

