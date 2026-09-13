package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

/** A slide in the landing-page hero carousel. CMS-editable, no redeploy needed. */
@Entity
@Table(name = "hero_slide", indexes = {
        @Index(name = "idx_hero_active_order", columnList = "active,sort_order")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HeroSlide extends BaseEntity {

    @Column(length = 60)
    private String eyebrow;

    @Column(nullable = false, length = 220)
    private String headline;

    @Column(name = "headline_mr", length = 220)
    private String headlineMr;

    @Column(length = 400)
    private String subheadline;

    @Column(name = "subheadline_mr", length = 400)
    private String subheadlineMr;

    @Column(name = "cta_label", length = 80)
    private String ctaLabel;

    @Column(name = "cta_url", length = 300)
    private String ctaUrl;

    @Column(name = "secondary_cta_label", length = 80)
    private String secondaryCtaLabel;

    @Column(name = "secondary_cta_url", length = 300)
    private String secondaryCtaUrl;

    /** Key into the generated SVG artwork set — avoids heavy binary assets in git. */
    @Column(name = "art_key", length = 60)
    private String artKey;

    @Column(name = "background_image_url", length = 400)
    private String backgroundImageUrl;

    /** CSS gradient fallback used before/instead of the image. */
    @Column(name = "gradient", length = 200)
    private String gradient;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
