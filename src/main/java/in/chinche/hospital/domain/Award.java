package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

/** Accreditation / recognition entry for the "Awards & Accreditation" band. */
@Entity
@Table(name = "award")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Award extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String issuer;

    @Column(name = "award_year", length = 20)
    private String awardYear;

    @Column(length = 600)
    private String description;

    @Column(name = "icon_key", length = 60)
    private String iconKey;

    @Column(name = "image_url", length = 400)
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
