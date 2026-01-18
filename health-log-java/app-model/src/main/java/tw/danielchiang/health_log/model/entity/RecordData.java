package tw.danielchiang.health_log.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 記錄數值實體 (EAV Value)
 * 對應資料表: record_data
 */
@Entity
@Table(name = "record_data",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_record_setting", columnNames = {"record_id", "setting_id"})
    },
    indexes = {
        @Index(name = "idx_record_data_setting_record", columnList = "setting_id, record_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_id")
    private Long dataId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    @JsonBackReference("dailyRecord")
    private DailyRecord dailyRecord;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setting_id", nullable = false)
    @JsonBackReference("fieldSetting")
    private FieldSetting fieldSetting;

    @Column(name = "value_text", nullable = false, columnDefinition = "TEXT")
    private String valueText; // 所有類型暫存為 TEXT
}

