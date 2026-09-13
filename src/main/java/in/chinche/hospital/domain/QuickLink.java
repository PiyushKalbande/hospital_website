package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

/** A service tile in the shortcut row under the booking widget. */
@Entity
@Table(name = "quick_link", indexes = {
        @Index(name = "idx_quicklink_group", columnList = "link_group,sort_order")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuickLink extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String label;

    @Column(name = "label_mr", length = 120)
    private String labelMr;

    @Column(length = 240)
    private String description;

    @Column(name = "description_mr", length = 240)
    private String descriptionMr;

    @Column(name = "icon_key", length = 60)
    private String iconKey;

    @Column(name = "link_url", length = 300, nullable = false)
    private String linkUrl;

    /** Placement bucket: HERO_SERVICES | FOOTER | PATIENT_CORNER */
    @Column(name = "link_group", length = 40)
    private String linkGroup;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
