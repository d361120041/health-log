package tw.danielchiang.health_log.data.repository;

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
import tw.danielchiang.health_log.model.entity.Role;
import tw.danielchiang.health_log.model.entity.User;

/**
 * UserRepository 測試
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = TestApplication.class)
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // 建立測試角色
        userRole = new Role();
        userRole.setRoleName("USER");
        entityManager.persistAndFlush(userRole);

        adminRole = new Role();
        adminRole.setRoleName("ADMIN");
        entityManager.persistAndFlush(adminRole);
    }

    @Test
    void testFindByEmail() {
        // Given: 建立測試使用者
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password");
        user.setRole(userRole);
        entityManager.persistAndFlush(user);

        // When: 根據 email 查詢
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then: 應該找到使用者
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testExistsByEmail() {
        // Given: 建立測試使用者
        User user = new User();
        user.setEmail("exists@example.com");
        user.setPasswordHash("hashed_password");
        user.setRole(userRole);
        entityManager.persistAndFlush(user);

        // When: 檢查 email 是否存在
        boolean exists = userRepository.existsByEmail("exists@example.com");
        boolean notExists = userRepository.existsByEmail("notexists@example.com");

        // Then: 應該正確判斷存在性
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testFindByEmailAndIsActive() {
        // Given: 建立啟用和停用的使用者
        User activeUser = new User();
        activeUser.setEmail("active@example.com");
        activeUser.setPasswordHash("hashed_password");
        activeUser.setRole(userRole);
        activeUser.setIsActive(true);
        entityManager.persistAndFlush(activeUser);

        User inactiveUser = new User();
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPasswordHash("hashed_password");
        inactiveUser.setRole(userRole);
        inactiveUser.setIsActive(false);
        entityManager.persistAndFlush(inactiveUser);

        // When: 查詢啟用的使用者
        Optional<User> foundActive = userRepository.findByEmailAndIsActive("active@example.com", true);
        Optional<User> foundInactive = userRepository.findByEmailAndIsActive("inactive@example.com", false);

        // Then: 應該正確找到對應的使用者
        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getIsActive()).isTrue();
        assertThat(foundInactive).isPresent();
        assertThat(foundInactive.get().getIsActive()).isFalse();
    }
}

