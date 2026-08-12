package vn.nguongocso.farm.entity;

import java.util.UUID;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity đại diện cho danh mục loại cây trồng.
 */
@Entity
@Table(name = "product_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategory {
	@Id
	@Column(name = "id", nullable = false, updatable = false)
	@JdbcTypeCode(SqlTypes.CHAR)
	private UUID id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "category_group")
	private String group;

	@Column(name = "description")
	private String description;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;
}