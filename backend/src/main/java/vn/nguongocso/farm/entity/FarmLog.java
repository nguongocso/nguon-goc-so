package vn.nguongocso.farm.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Nhật ký hoạt động sản xuất của lô sản xuất.
 */
@Entity
@Table(name = "farm_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmLog {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "production_lot_id", nullable = false)
	private ProductionLot productionLotId;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_type", nullable = false)
	private FarmActivityType activityType;

	@Column(name = "material")
	private String material;

	@Column(name = "quantity")
	private Double quantity;

	@Column(name = "unit")
	private String unit;

	@Column(name = "executed_date", nullable = false)
	private LocalDate executedDate;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	/**
	 * Bản gốc của nhật ký khi bản ghi này là bản đính chính
	 * (NCL-03-CN-006). Mọi bản đính chính đều trỏ trực tiếp tới bản gốc.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "original_farm_log_id")
	private FarmLog originalFarmLogId;

	/**
	 * Đánh dấu bản ghi là bản đính chính (true) hay bản ghi thường (false).
	 */
	@Column(name = "is_correction", nullable = false)
	private boolean isCorrection = false;

	/**
	 * Lý do đính chính (bắt buộc khi là bản đính chính).
	 */
	@Column(name = "correction_reason", columnDefinition = "TEXT")
	private String correctionReason;

	/**
	 * Người thực hiện đính chính (khác người ghi nếu quản lý VT-02 sửa).
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "corrected_by")
	private User correctedBy;

	/**
	 * Đánh dấu bản ghi đã bị thay thế hiệu lực bởi một bản đính chính khác.
	 */
	@Column(name = "is_corrected", nullable = false)
	private boolean isCorrected = false;

	/**
	 * Setter tường minh cho cờ đính chính (Lombok sinh tên isCorrection()
	 * cho trường boolean có tiền tố "is").
	 */
	public void setIsCorrection(boolean correction) {
		this.isCorrection = correction;
	}

	/**
	 * Setter tường minh cho cờ đã bị đính chính.
	 */
	public void setIsCorrected(boolean corrected) {
		this.isCorrected = corrected;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}

		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}