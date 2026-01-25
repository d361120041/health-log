package tw.danielchiang.health_log.data.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import tw.danielchiang.health_log.model.entity.User;

/**
 * 使用者 Repository
 */
@Repository
public interface UserRepository extends BaseRepository<User, Long> {

    /**
     * 根據 email 查詢使用者
     * @param email 電子郵件
     * @return 使用者實體
     */
    Optional<User> findByEmail(String email);

    /**
     * 檢查 email 是否存在
     * @param email 電子郵件
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根據 email 和啟用狀態查詢使用者
     * @param email 電子郵件
     * @param isActive 是否啟用
     * @return 使用者實體
     */
    Optional<User> findByEmailAndIsActive(String email, Boolean isActive);

    /**
     * 根據 Email 驗證 Token 查詢使用者
     * @param token Email 驗證 Token
     * @return 使用者實體
     */
    Optional<User> findByEmailVerificationToken(String token);

    /**
     * 根據 OAuth2 提供者和 OAuth2 ID 查詢使用者
     * @param oauth2Provider OAuth2 提供者（如 'GOOGLE'）
     * @param oauth2Id OAuth2 使用者 ID
     * @return 使用者實體
     */
    Optional<User> findByOauth2ProviderAndOauth2Id(String oauth2Provider, String oauth2Id);
}

