package tw.danielchiang.health_log.data.repository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

import tw.danielchiang.health_log.data.config.TestApplication;
import tw.danielchiang.health_log.model.entity.FieldSetting;

/**
 * FieldSettingRepository 測試
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = TestApplication.class)
class FieldSettingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FieldSettingRepository fieldSettingRepository;

    @Test
    void testFindByIsActiveTrue() {
        // Given: 建立啟用和停用的欄位設定
        FieldSetting active1 = new FieldSetting();
        active1.setFieldName("Active Field 1");
        active1.setDataType("NUMBER");
        active1.setIsActive(true);
        entityManager.persistAndFlush(active1);

        FieldSetting active2 = new FieldSetting();
        active2.setFieldName("Active Field 2");
        active2.setDataType("TEXT");
        active2.setIsActive(true);
        entityManager.persistAndFlush(active2);

        FieldSetting inactive = new FieldSetting();
        inactive.setFieldName("Inactive Field");
        inactive.setDataType("NUMBER");
        inactive.setIsActive(false);
        entityManager.persistAndFlush(inactive);

        // When: 查詢所有啟用的欄位設定
        List<FieldSetting> activeFields = fieldSettingRepository.findByIsActiveTrue();

        // Then: 應該只返回啟用的欄位
        assertThat(activeFields).hasSize(2);
        assertThat(activeFields).allMatch(FieldSetting::getIsActive);
    }

    @Test
    void testFindByFieldName() {
        // Given: 建立測試欄位設定
        FieldSetting field = new FieldSetting();
        field.setFieldName("Test Field");
        field.setDataType("NUMBER");
        field.setUnit("kg");
        entityManager.persistAndFlush(field);

        // When: 根據欄位名稱查詢
        Optional<FieldSetting> found = fieldSettingRepository.findByFieldName("Test Field");

        // Then: 應該找到欄位設定
        assertThat(found).isPresent();
        assertThat(found.get().getFieldName()).isEqualTo("Test Field");
        assertThat(found.get().getDataType()).isEqualTo("NUMBER");
    }

    @Test
    void testExistsByFieldName() {
        // Given: 建立測試欄位設定
        FieldSetting field = new FieldSetting();
        field.setFieldName("Exists Field");
        field.setDataType("TEXT");
        entityManager.persistAndFlush(field);

        // When: 檢查欄位名稱是否存在
        boolean exists = fieldSettingRepository.existsByFieldName("Exists Field");
        boolean notExists = fieldSettingRepository.existsByFieldName("Not Exists");

        // Then: 應該正確判斷存在性
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}

