package tw.danielchiang.health_log.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tw.danielchiang.health_log.model.entity.FieldSetting;

/**
 * 欄位設定 Repository
 */
@Repository
public interface FieldSettingRepository extends JpaRepository<FieldSetting, Integer> {

    /**
     * 查詢所有欄位設定，按 setting_id 升序排序
     * @return 所有欄位設定列表
     */
    List<FieldSetting> findAllByOrderBySettingIdAsc();

    /**
     * 查詢所有啟用的欄位設定
     * @return 啟用的欄位設定列表
     */
    List<FieldSetting> findByIsActiveTrue();

    /**
     * 查詢所有啟用的欄位設定，按 setting_id 升序排序
     * @return 啟用的欄位設定列表
     */
    List<FieldSetting> findByIsActiveTrueOrderBySettingIdAsc();

    /**
     * 根據欄位名稱查詢
     * @param fieldName 欄位名稱
     * @return 欄位設定實體
     */
    Optional<FieldSetting> findByFieldName(String fieldName);

    /**
     * 檢查欄位名稱是否存在
     * @param fieldName 欄位名稱
     * @return 是否存在
     */
    boolean existsByFieldName(String fieldName);
}

