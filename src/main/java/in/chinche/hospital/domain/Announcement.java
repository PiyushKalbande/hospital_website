package in.chinche.hospital.domain;

import in.chinche.hospital.domain.enums.ContentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** News item shown in the marquee strip and the "Hospital Updates" band. */
@Entity
@Table(name = "announcement", indexes = {
        @Index(name = "idx_announcement_slug", columnList = "slug", unique = true),
        @Index(name = "idx_announcement_published", columnList = "status,published_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Announcement extends BaseEntity {

    @Column(nullable = false, length = 220)
    private String title;

    @Column(name = "title_mr", length = 220)
    private String titleMr;

    @Column(nullable = false, length = 240, unique = true)
    private String slug;

    @Column(length = 600)
    private String summary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "image_url", length = 400)
    private String imageUrl;

    /** Short kicker label, e.g. "NEW TECHNOLOGY", "FREE CAMP". */
    @Column(name = "badge", length = 60)
    private String badge;

    @Column(name = "link_url", length = 400)
    private String linkUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "pinned")
    private boolean pinned;

    @Column(nullable = false)
    private boolean active = true;
}
