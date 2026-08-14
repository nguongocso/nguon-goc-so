package vn.nguongocso.organization.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Đại diện cho một tổ chức trong hệ thống.
 *
 * <p>
 * Mỗi tổ chức có mã định danh duy nhất, tên, mã tổ chức, loại tổ chức, trạng
 * thái và các thông tin liên hệ. Dữ liệu thời gian tạo, cập nhật và UUID được
 * tự động khởi tạo thông qua các callback của JPA.
 * </p>
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {
	@Id
	@Column(name = "organization_id")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID organizationId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrganizationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrganizationStatus status;

	@Column
	private String address;

	@Column
	private String phone;

	@Column
	private String email;

	@Column(nullable = false, updatable = false, name = "created_at")
	private LocalDateTime createdAt;

	@Column(nullable = false, name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		if (organizationId == null) {
			organizationId = UUID.randomUUID();
		}
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		if (status == null) {
			status = OrganizationStatus.ACTIVE;
		}
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}