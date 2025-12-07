package tw.danielchiang.health_log.data.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tw.danielchiang.health_log.model.entity.User;

/**
 * 使用者 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

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
}

