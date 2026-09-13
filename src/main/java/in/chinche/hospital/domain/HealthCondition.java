package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

/** A common condition used for the bilingual "conditions we treat" grid. */
@Entity
@Table(name = "health_condition", indexes = {
        @Index(name = "idx_condition_slug", columnList = "slug", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthCondition extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_mr", length = 120)
    private String nameMr;

    @Column(nullable = false, length = 140, unique = true)
    private String slug;

    @Column(name = "icon_key", length = 60)
    private String iconKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "speciality_id")
    private Speciality speciality;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
