package vn.nguongocso.farm.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.nguongocso.farm.enums.MaterialGroup;

/**
 * Entity đại diện cho danh mục vật tư đầu vào kèm thời gian cách ly.
 */
@Entity
@Table(name = "input_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputMaterial {
	@Id
	@Column(name = "id", nullable = false, updatable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Column(name = "name", nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "material_group", nullable = false, length = 50)
	private MaterialGroup materialGroup;

	@Column(name = "active_ingredient")
	private String activeIngredient;

	@Column(name = "unit", nullable = false, length = 50)
	private String unit;

	@Column(name = "quarantine_days", nullable = false)
	private Integer quarantineDays;

	@Column(name = "apply_to_all_crops", nullable = false)
	private Boolean applyToAllCrops;

	@Column(name = "reference_source", columnDefinition = "TEXT")
	private String referenceSource;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_by")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Builder.Default
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "input_material_crop_types",
		joinColumns = @JoinColumn(name = "material_id"),
		inverseJoinColumns = @JoinColumn(name = "crop_category_id")
	)
	private Set<ProductCategory> applicableCropTypes = new HashSet<>();

	@PrePersist
	public void prePersist() {
		if (this.id == null) {
			this.id = UUID.randomUUID();
		}
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
		if (this.isActive == null) {
			this.isActive = true;
		}
		if (this.applyToAllCrops == null) {
			this.applyToAllCrops = true;
		}
		if (this.quarantineDays == null) {
			this.quarantineDays = 0;
		}
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
