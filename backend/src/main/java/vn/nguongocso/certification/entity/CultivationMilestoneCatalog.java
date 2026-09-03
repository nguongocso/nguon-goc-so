package vn.nguongocso.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Master catalog of cultivation milestones.
 * Story: NCL-09-CN-011
 */
@Entity
@Table(
    name = "cultivation_milestone_catalog",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_milestone_name_activity",
            columnNames = {"name", "activity_type"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CultivationMilestoneCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;

    @Column(name = "expected_days_from_planting")
    private Integer expectedDaysFromPlanting;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
