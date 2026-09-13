package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** An individual investigation bookable from the Lab section. */
@Entity
@Table(name = "lab_test", indexes = {
        @Index(name = "idx_labtest_slug", columnList = "slug", unique = true),
        @Index(name = "idx_labtest_category", columnList = "category")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabTest extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "name_mr", length = 160)
    private String nameMr;

    @Column(nullable = false, length = 180, unique = true)
    private String slug;

    @Column(length = 80)
    private String category;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "sample_type", length = 80)
    private String sampleType;

    @Column(name = "report_hours")
    private Integer reportHours;

    @Column(name = "fasting_required")
    private boolean fastingRequired;

    @Column(name = "parameters", length = 400)
    private String parameters;

    @Column(name = "home_collection")
    private boolean homeCollection = true;

    @Column(nullable = false)
    private boolean active = true;
}
