package vn.nguongocso.permission.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.organization.entity.Organization;

import java.time.LocalDateTime;
import java.util.UUID;

// Thực thể OrganizationRolePermission đại diện cho mối quan hệ giữa vai trò (Role) và quyền (Permission) trong một tổ chức (Organization). Nó xác định quyền hạn mà một vai trò cụ thể có trong tổ chức đó.
@Entity
@Table(name = "organization_role_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRolePermission {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (enabled == null) {
            enabled = Boolean.FALSE;
        }

        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}