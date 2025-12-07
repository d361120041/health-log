package tw.danielchiang.health_log.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tw.danielchiang.health_log.model.entity.RecordData;

/**
 * 記錄數值 Repository
 */
@Repository
public interface RecordDataRepository extends JpaRepository<RecordData, Long> {

    /**
     * 根據記錄 ID 查詢所有記錄數值
     * @param recordId 記錄 ID
     * @return 記錄數值列表
     */
    List<RecordData> findByDailyRecordRecordId(Long recordId);

    /**
     * 根據記錄 ID 刪除所有記錄數值
     * @param recordId 記錄 ID
     */
    @Modifying
    @Query("DELETE FROM RecordData rd WHERE rd.dailyRecord.recordId = :recordId")
    void deleteByRecordId(@Param("recordId") Long recordId);

    /**
     * 根據欄位設定 ID 和記錄 ID 查詢
     * @param settingId 欄位設定 ID
     * @param recordId 記錄 ID
     * @return 記錄數值實體
     */
    @Query("SELECT rd FROM RecordData rd WHERE rd.fieldSetting.settingId = :settingId AND rd.dailyRecord.recordId = :recordId")
    Optional<RecordData> findBySettingIdAndRecordId(@Param("settingId") Integer settingId, @Param("recordId") Long recordId);
}

