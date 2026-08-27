package vn.nguongocso.organization.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.auth.entity.User;

/**
 * Bản ghi gán địa bàn quản lý cho tài khoản (nhiều-nhiều tài khoản ↔ đơn vị
 * hành chính).
 *
 * <p>
 * Ràng buộc UNIQUE (user_id, unit_id) ở tầng DB chặn gán trùng ngay cả khi có
 * race-condition giữa 2 request song song.
 * </p>
 */
@Entity
@Table(
        name = "user_area_assignments",
        // Trùng khớp ràng buộc UNIQUE trong V35 để schema do Hibernate sinh ra
        // (profile test, create-drop) cũng chặn gán trùng như production.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_area_assignment",
                columnNames = { "user_id", "unit_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAreaAssignment {
	@Id
	@Column(name = "id")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	/** Tài khoản được gán địa bàn. */
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** Đơn vị hành chính được gán. */
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "unit_id", nullable = false)
	private AdministrativeUnit unit;

	/** Thời điểm gán. */
	@Column(nullable = false, name = "assigned_at")
	private LocalDateTime assignedAt;

	/** Người thao tác gán/gỡ (nullable). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_by")
	private User assignedBy;

	@PrePersist
	public void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (assignedAt == null) {
			assignedAt = LocalDateTime.now();
		}
	}
}
