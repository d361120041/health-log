package tw.danielchiang.health_log.data.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import tw.danielchiang.health_log.model.entity.Role;

/**
 * 角色 Repository
 */
@Repository
public interface RoleRepository extends BaseRepository<Role, Integer> {

    /**
     * 根據角色名稱查詢
     * @param roleName 角色名稱
     * @return 角色實體
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * 檢查角色名稱是否存在
     * @param roleName 角色名稱
     * @return 是否存在
     */
    boolean existsByRoleName(String roleName);
}

