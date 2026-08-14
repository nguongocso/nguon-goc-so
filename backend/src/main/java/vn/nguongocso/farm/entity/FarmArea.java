package vn.nguongocso.farm.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.organization.entity.Organization;

/**
 * Entity đại diện cho vùng trồng thuộc một tổ chức.
 */
@Entity
@Table(name = "farm_areas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmArea {
	@Id
	@Column(name = "id", nullable = false, updatable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "location", columnDefinition = "POINT")
	private Point location;

	@Column(name = "area", nullable = false)
	private BigDecimal area;

	@Enumerated(EnumType.STRING)
	@Column(name = "area_unit", nullable = false)
	private AreaUnit areaUnit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "crop_type", nullable = false)
	private ProductCategory cropType;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}

		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}