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