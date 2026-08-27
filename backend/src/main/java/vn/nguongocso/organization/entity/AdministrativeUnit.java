package vn.nguongocso.organization.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;

/**
 * Đơn vị hành chính trong danh mục dùng chung (cấp tỉnh / cấp xã).
 *
 * <p>
 * Cây tự tham chiếu qua {@code parent_id}; {@code province_id} denormalize
 * trỏ về đơn vị gốc cấp tỉnh để lọc "toàn bộ đơn vị dưới một tỉnh" mà không
 * phải duyệt cây. Mã đơn vị theo bảng mã hành chính quốc gia
 * (Quyết định 19/2025/QĐ-TTg, hiệu lực 01/07/2025).
 * </p>
 */
@Entity
@Table(name = "administrative_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdministrativeUnit {
	@Id
	@Column(name = "id")
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	/** Mã hành chính chính thức (duy nhất toàn quốc). */
	@Column(nullable = false, unique = true, length = 20)
	private String code;

	/** Tên đơn vị (không kèm tiền tố "Tỉnh"/"Thành phố"/"Phường"/"Xã"). */
	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AdministrativeUnitLevel level;

	/** Đơn vị cha (NULL với cấp tỉnh). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private AdministrativeUnit parent;

	/** Đơn vị gốc cấp tỉnh chứa đơn vị này (NULL với chính cấp tỉnh). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "province_id")
	private AdministrativeUnit province;

	@Column(nullable = false)
	@Builder.Default
	private boolean active = true;

	@PrePersist
	public void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
	}
}
