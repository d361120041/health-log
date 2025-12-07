package tw.danielchiang.health_log.data.repository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import tw.danielchiang.health_log.data.config.TestApplication;
import tw.danielchiang.health_log.model.entity.Role;

/**
 * RoleRepository 測試
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = TestApplication.class)
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testFindByRoleName() {
        // Given: 建立測試角色
        Role role = new Role();
        role.setRoleName("TEST_ROLE");
        roleRepository.save(role);

        // When: 根據角色名稱查詢
        Optional<Role> found = roleRepository.findByRoleName("TEST_ROLE");

        // Then: 應該找到角色
        assertThat(found).isPresent();
        assertThat(found.get().getRoleName()).isEqualTo("TEST_ROLE");
    }

    @Test
    void testExistsByRoleName() {
        // Given: 建立測試角色
        Role role = new Role();
        role.setRoleName("EXISTS_ROLE");
        roleRepository.save(role);

        // When: 檢查角色是否存在
        boolean exists = roleRepository.existsByRoleName("EXISTS_ROLE");
        boolean notExists = roleRepository.existsByRoleName("NOT_EXISTS");

        // Then: 應該正確判斷存在性
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testSaveRole() {
        // Given: 建立新角色
        Role role = new Role();
        role.setRoleName("NEW_ROLE");

        // When: 儲存角色
        Role saved = roleRepository.save(role);

        // Then: 應該成功儲存並有 ID
        assertThat(saved.getRoleId()).isNotNull();
        assertThat(saved.getRoleName()).isEqualTo("NEW_ROLE");
    }
}

