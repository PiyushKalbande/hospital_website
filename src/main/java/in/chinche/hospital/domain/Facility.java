package in.chinche.hospital.domain;

import in.chinche.hospital.domain.enums.FacilityType;
import jakarta.persistence.*;
import lombok.*;

/** A physical campus: the Katol main hospital, plus satellite clinics and centres. */
@Entity
@Table(name = "facility", indexes = {
        @Index(name = "idx_facility_slug", columnList = "slug", unique = true),
        @Index(name = "idx_facility_city", columnList = "city")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Facility extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "name_mr", length = 160)
    private String nameMr;

    @Column(nullable = false, length = 180, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", length = 40)
    private FacilityType facilityType;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String district;

    @Column(length = 120)
    private String state;

    @Column(length = 20)
    private String pincode;

    @Column(length = 300)
    private String addressLine;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String emergencyPhone;

    @Column(length = 160)
    private String email;

    private Double latitude;
    private Double longitude;

    /** Public rating shown on the facility card (0–5). */
    @Column(name = "rating")
    private Double rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "bed_count")
    private Integer bedCount;

    @Column(name = "image_url", length = 400)
    private String imageUrl;

    /** Comma-separated amenity codes resolved by the frontend, e.g. "icu,ot,pharmacy". */
    @Column(name = "amenities", length = 400)
    private String amenities;

    @Column(name = "hours_text", length = 200)
    private String hoursText;

    @Column(name = "is_headquarter")
    private boolean headquarter;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
