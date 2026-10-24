package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

/** A named intervention offered within a speciality (e.g. Total Knee Replacement). */
@Entity
@Table(name = "hospital_procedure", indexes = {
        @Index(name = "idx_procedure_slug", columnList = "slug", unique = true),
        @Index(name = "idx_procedure_speciality", columnList = "speciality_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Procedure extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "name_mr", length = 160)
    private String nameMr;

    @Column(nullable = false, length = 180, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "speciality_id")
    private Speciality speciality;

    @Column(name = "icon_key", length = 60)
    private String iconKey;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Minimum indicative cost in INR; 0/null renders as "On request". */
    @Column(name = "indicative_cost")
    private Integer indicativeCost;

    @Column(name = "stay_days")
    private Integer typicalStayDays;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
