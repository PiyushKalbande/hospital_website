package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** A preventive health check-up package sold through the booking widget. */
@Entity
@Table(name = "health_package", indexes = {
        @Index(name = "idx_package_slug", columnList = "slug", unique = true),
        @Index(name = "idx_package_featured", columnList = "featured")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthPackage extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "name_mr", length = 160)
    private String nameMr;

    @Column(nullable = false, length = 180, unique = true)
    private String slug;

    /** BASIC | ESSENTIAL | ADVANCED | PREMIUM — controls card styling. */
    @Column(name = "tier", length = 30)
    private String tier;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "list_price", precision = 10, scale = 2)
    private BigDecimal listPrice;

    @Column(length = 60)
    private String tagline;

    @Column(name = "tagline_mr", length = 60)
    private String taglineMr;

    /** Newline-separated list of included investigations. */
    @Column(name = "includes", columnDefinition = "TEXT")
    private String includes;

    @Column(name = "test_count")
    private Integer testCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_mr", columnDefinition = "TEXT")
    private String descriptionMr;

    /** "male" | "female" | "all" — used for age/gender targeting on the widget. */
    @Column(name = "gender_suitability", length = 20)
    private String genderSuitability = "all";

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "fasting_required")
    private boolean fastingRequired = true;

    @Column(name = "report_hours")
    private Integer reportHours = 24;

    @Column(name = "home_collection")
    private boolean homeCollection = true;

    @Column(name = "icon_key", length = 60)
    private String iconKey;

    @Column(name = "featured")
    private boolean featured;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
