package tw.danielchiang.health_log.data.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import tw.danielchiang.health_log.model.entity.Role;
import tw.danielchiang.health_log.model.entity.User;

/**
 * DailyRecordRepository 測試
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = TestApplication.class)
class DailyRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DailyRecordRepository dailyRecordRepository;

    private User testUser;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        // 建立測試角色和使用者
        Role userRole = new Role();
        userRole.setRoleName("USER");
        entityManager.persistAndFlush(userRole);

        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");
        testUser.setRole(userRole);
        entityManager.persistAndFlush(testUser);

        testDate = LocalDate.now();
    }

    @Test
    void testFindByUserIdAndRecordDate() {
        // Given: 建立測試記錄
        DailyRecord record = new DailyRecord();
        record.setUser(testUser);
        record.setRecordDate(testDate);
        entityManager.persistAndFlush(record);

        // When: 根據使用者 ID 和日期查詢
        Optional<DailyRecord> found = dailyRecordRepository.findByUserIdAndRecordDate(
            testUser.getId(), testDate);

        // Then: 應該找到記錄
        assertThat(found).isPresent();
        assertThat(found.get().getRecordDate()).isEqualTo(testDate);
        assertThat(found.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void testExistsByUserIdAndRecordDate() {
        // Given: 建立測試記錄
        DailyRecord record = new DailyRecord();
        record.setUser(testUser);
        record.setRecordDate(testDate);
        entityManager.persistAndFlush(record);

        // When: 檢查記錄是否存在
        boolean exists = dailyRecordRepository.existsByUserIdAndRecordDate(
            testUser.getId(), testDate);
        boolean notExists = dailyRecordRepository.existsByUserIdAndRecordDate(
            testUser.getId(), LocalDate.now().plusDays(1));

        // Then: 應該正確判斷存在性
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testFindByUserIdOrderByRecordDateDesc() {
        // Given: 建立多筆不同日期的記錄
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().plusDays(1);
        LocalDate date3 = LocalDate.now().plusDays(2);

        DailyRecord record1 = new DailyRecord();
        record1.setUser(testUser);
        record1.setRecordDate(date1);
        entityManager.persistAndFlush(record1);

        DailyRecord record2 = new DailyRecord();
        record2.setUser(testUser);
        record2.setRecordDate(date2);
        entityManager.persistAndFlush(record2);

        DailyRecord record3 = new DailyRecord();
        record3.setUser(testUser);
        record3.setRecordDate(date3);
        entityManager.persistAndFlush(record3);

        // When: 查詢該使用者的所有記錄
        List<DailyRecord> records = dailyRecordRepository.findByUserIdOrderByRecordDateDesc(
            testUser.getId());

        // Then: 應該按日期降序排列
        assertThat(records).hasSize(3);
        assertThat(records.get(0).getRecordDate()).isEqualTo(date3);
        assertThat(records.get(1).getRecordDate()).isEqualTo(date2);
        assertThat(records.get(2).getRecordDate()).isEqualTo(date1);
    }

    @Test
    void testUniqueConstraint() {
        // Given: 建立一筆記錄
        DailyRecord record1 = new DailyRecord();
        record1.setUser(testUser);
        record1.setRecordDate(testDate);
        entityManager.persistAndFlush(record1);

        // When: 嘗試建立相同使用者、相同日期的記錄
        DailyRecord record2 = new DailyRecord();
        record2.setUser(testUser);
        record2.setRecordDate(testDate);

        // Then: 應該拋出唯一約束違反異常
        try {
            entityManager.persistAndFlush(record2);
            entityManager.flush();
            // 如果沒有拋出異常，測試失敗
            assertThat(false).as("應該拋出唯一約束違反異常").isTrue();
        } catch (Exception e) {
            // 預期會拋出異常
            assertThat(e).isNotNull();
        }
    }
}

