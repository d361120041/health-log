package tw.danielchiang.health_log.data.repository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

import tw.danielchiang.health_log.data.config.TestApplication;
import tw.danielchiang.health_log.model.entity.DailyRecord;
import tw.danielchiang.health_log.model.entity.FieldSetting;
import tw.danielchiang.health_log.model.entity.RecordData;
import tw.danielchiang.health_log.model.entity.Role;
import tw.danielchiang.health_log.model.entity.User;

/**
 * RecordDataRepository 測試
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = TestApplication.class)
class RecordDataRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecordDataRepository recordDataRepository;

    @Autowired
    private DailyRecordRepository dailyRecordRepository;

    private DailyRecord testRecord;
    private FieldSetting testFieldSetting;

    @BeforeEach
    void setUp() {
        // 建立測試角色和使用者
        Role userRole = new Role();
        userRole.setRoleName("USER");
        entityManager.persistAndFlush(userRole);

        User testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");
        testUser.setRole(userRole);
        entityManager.persistAndFlush(testUser);

        // 建立測試記錄
        testRecord = new DailyRecord();
        testRecord.setUser(testUser);
        testRecord.setRecordDate(LocalDate.now());
        entityManager.persistAndFlush(testRecord);

        // 建立測試欄位設定
        testFieldSetting = new FieldSetting();
        testFieldSetting.setFieldName("Test Field");
        testFieldSetting.setDataType("NUMBER");
        testFieldSetting.setUnit("kg");
        entityManager.persistAndFlush(testFieldSetting);
    }

    @Test
    void testFindByDailyRecordRecordId() {
        // Given: 建立多筆記錄數值
        RecordData data1 = new RecordData();
        data1.setDailyRecord(testRecord);
        data1.setFieldSetting(testFieldSetting);
        data1.setValueText("100");
        entityManager.persistAndFlush(data1);

        FieldSetting field2 = new FieldSetting();
        field2.setFieldName("Test Field 2");
        field2.setDataType("TEXT");
        entityManager.persistAndFlush(field2);

        RecordData data2 = new RecordData();
        data2.setDailyRecord(testRecord);
        data2.setFieldSetting(field2);
        data2.setValueText("Some text");
        entityManager.persistAndFlush(data2);

        // When: 根據記錄 ID 查詢
        List<RecordData> recordDataList = recordDataRepository.findByDailyRecordRecordId(
            testRecord.getRecordId());

        // Then: 應該找到所有相關的記錄數值
        assertThat(recordDataList).hasSize(2);
        assertThat(recordDataList).allMatch(rd -> 
            rd.getDailyRecord().getRecordId().equals(testRecord.getRecordId()));
    }

    @Test
    void testDeleteByRecordId() {
        // Given: 建立記錄數值
        RecordData data = new RecordData();
        data.setDailyRecord(testRecord);
        data.setFieldSetting(testFieldSetting);
        data.setValueText("100");
        entityManager.persistAndFlush(data);

        // When: 刪除該記錄的所有數值
        recordDataRepository.deleteByRecordId(testRecord.getRecordId());
        entityManager.flush();
        entityManager.clear();

        // Then: 應該已刪除
        List<RecordData> remaining = recordDataRepository.findByDailyRecordRecordId(
            testRecord.getRecordId());
        assertThat(remaining).isEmpty();
    }

    @Test
    void testFindBySettingIdAndRecordId() {
        // Given: 建立記錄數值
        RecordData data = new RecordData();
        data.setDailyRecord(testRecord);
        data.setFieldSetting(testFieldSetting);
        data.setValueText("100");
        entityManager.persistAndFlush(data);

        // When: 根據欄位設定 ID 和記錄 ID 查詢
        var found = recordDataRepository.findBySettingIdAndRecordId(
            testFieldSetting.getSettingId(), testRecord.getRecordId());

        // Then: 應該找到記錄數值
        assertThat(found).isPresent();
        assertThat(found.get().getValueText()).isEqualTo("100");
        assertThat(found.get().getFieldSetting().getSettingId())
            .isEqualTo(testFieldSetting.getSettingId());
    }

    @Test
    void testUniqueConstraint() {
        // Given: 建立一筆記錄數值
        RecordData data1 = new RecordData();
        data1.setDailyRecord(testRecord);
        data1.setFieldSetting(testFieldSetting);
        data1.setValueText("100");
        entityManager.persistAndFlush(data1);

        // When: 嘗試建立相同記錄、相同欄位設定的記錄數值
        RecordData data2 = new RecordData();
        data2.setDailyRecord(testRecord);
        data2.setFieldSetting(testFieldSetting);
        data2.setValueText("200");

        // Then: 應該拋出唯一約束違反異常
        try {
            entityManager.persistAndFlush(data2);
            entityManager.flush();
            // 如果沒有拋出異常，測試失敗
            assertThat(false).as("應該拋出唯一約束違反異常").isTrue();
        } catch (Exception e) {
            // 預期會拋出異常
            assertThat(e).isNotNull();
        }
    }

    @Test
    void testCascadeDelete() {
        // Given: 建立記錄數值
        RecordData data = new RecordData();
        data.setDailyRecord(testRecord);
        data.setFieldSetting(testFieldSetting);
        data.setValueText("100");
        entityManager.persistAndFlush(data);

        Long dataId = data.getDataId();
        Long recordId = testRecord.getRecordId();
        
        // 清除 EntityManager 以確保從資料庫重新載入
        entityManager.clear();

        // When: 刪除記錄（使用 Repository 的 deleteById，它會自動處理級聯）
        dailyRecordRepository.deleteById(recordId);
        entityManager.flush();
        entityManager.clear();

        // Then: 記錄數值應該被級聯刪除
        var found = recordDataRepository.findById(dataId);
        assertThat(found).isEmpty();
    }
}

