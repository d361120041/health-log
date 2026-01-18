package tw.danielchiang.health_log.model.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 欄位設定實體 (EAV Attribute)
 * 對應資料表: field_settings
 */
@Entity
@Table(name = "field_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Integer settingId;

    @Column(name = "field_name", nullable = false, unique = true, length = 100)
    private String fieldName;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType; // NUMBER, TEXT, ENUM

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "options", columnDefinition = "TEXT")
    private String options; // ENUM 類型時的選項列表 (JSON 或逗號分隔)

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "fieldSetting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("fieldSetting")
    private List<RecordData> recordDataList;

    @PrePersist
    protected void onCreate() {
        if (isRequired == null) {
            isRequired = false;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}

