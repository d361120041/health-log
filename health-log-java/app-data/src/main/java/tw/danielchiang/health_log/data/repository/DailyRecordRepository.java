package tw.danielchiang.health_log.data.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tw.danielchiang.health_log.model.entity.DailyRecord;

/**
 * 每日記錄 Repository
 */
@Repository
public interface DailyRecordRepository extends JpaRepository<DailyRecord, Long> {

    /**
     * 根據使用者 ID 和記錄日期查詢
     * @param userId 使用者 ID
     * @param recordDate 記錄日期
     * @return 每日記錄實體
     */
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.user.id = :userId AND dr.recordDate = :recordDate")
    Optional<DailyRecord> findByUserIdAndRecordDate(@Param("userId") Long userId, @Param("recordDate") LocalDate recordDate);

    /**
     * 檢查使用者是否在指定日期已有記錄
     * @param userId 使用者 ID
     * @param recordDate 記錄日期
     * @return 是否存在
     */
    @Query("SELECT COUNT(dr) > 0 FROM DailyRecord dr WHERE dr.user.id = :userId AND dr.recordDate = :recordDate")
    boolean existsByUserIdAndRecordDate(@Param("userId") Long userId, @Param("recordDate") LocalDate recordDate);

    /**
     * 根據使用者 ID 查詢所有記錄
     * @param userId 使用者 ID
     * @return 記錄列表
     */
    List<DailyRecord> findByUserIdOrderByRecordDateDesc(Long userId);
}

