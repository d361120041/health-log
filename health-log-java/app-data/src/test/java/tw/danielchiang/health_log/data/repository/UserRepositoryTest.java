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

    @Test
    void testFindByEmailVerificationToken() {
        // Given: 建立帶有 email 驗證 token 的使用者
        User user = new User();
        user.setEmail("verify@example.com");
        user.setPasswordHash("hashed_password");
        user.setRole(userRole);
        user.setEmailVerificationToken("verification-token-123");
        entityManager.persistAndFlush(user);

        // When: 根據 token 查詢使用者
        Optional<User> found = userRepository.findByEmailVerificationToken("verification-token-123");
        Optional<User> notFound = userRepository.findByEmailVerificationToken("non-existent-token");

        // Then: 應該正確找到對應的使用者
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("verify@example.com");
        assertThat(found.get().getEmailVerificationToken()).isEqualTo("verification-token-123");
        assertThat(notFound).isEmpty();
    }

    @Test
    void testFindByOauth2ProviderAndOauth2Id() {
        // Given: 建立 OAuth2 使用者
        User oauth2User = new User();
        oauth2User.setEmail("oauth2@example.com");
        oauth2User.setPasswordHash(null); // OAuth2 使用者沒有密碼
        oauth2User.setRole(userRole);
        oauth2User.setOauth2Provider("GOOGLE");
        oauth2User.setOauth2Id("google-oauth2-id-123");
        entityManager.persistAndFlush(oauth2User);

        // When: 根據 OAuth2 提供者和 ID 查詢使用者
        Optional<User> found = userRepository.findByOauth2ProviderAndOauth2Id("GOOGLE", "google-oauth2-id-123");
        Optional<User> notFound = userRepository.findByOauth2ProviderAndOauth2Id("GOOGLE", "non-existent-id");
        Optional<User> notFoundProvider = userRepository.findByOauth2ProviderAndOauth2Id("FACEBOOK", "google-oauth2-id-123");

        // Then: 應該正確找到對應的使用者
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("oauth2@example.com");
        assertThat(found.get().getOauth2Provider()).isEqualTo("GOOGLE");
        assertThat(found.get().getOauth2Id()).isEqualTo("google-oauth2-id-123");
        assertThat(found.get().getPasswordHash()).isNull();
        assertThat(notFound).isEmpty();
        assertThat(notFoundProvider).isEmpty();
    }

    @Test
    void testUserWithNullPasswordHash() {
        // Given: 建立沒有密碼的使用者（OAuth2 使用者）
        User userWithoutPassword = new User();
        userWithoutPassword.setEmail("oauth@example.com");
        userWithoutPassword.setPasswordHash(null); // password_hash 可以為 null
        userWithoutPassword.setRole(userRole);
        entityManager.persistAndFlush(userWithoutPassword);

        // When: 查詢使用者
        Optional<User> found = userRepository.findByEmail("oauth@example.com");

        // Then: 應該找到使用者，且 password_hash 為 null
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("oauth@example.com");
        assertThat(found.get().getPasswordHash()).isNull();
    }
}

