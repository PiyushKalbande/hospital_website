package in.chinche.hospital.domain;

import in.chinche.hospital.domain.enums.ContentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Health-education article powering the "Health Blogs" band. */
@Entity
@Table(name = "blog", indexes = {
        @Index(name = "idx_blog_slug", columnList = "slug", unique = true),
        @Index(name = "idx_blog_status_published", columnList = "status,published_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Blog extends BaseEntity {

    @Column(nullable = false, length = 220)
    private String title;

    @Column(name = "title_mr", length = 220)
    private String titleMr;

    @Column(nullable = false, length = 240, unique = true)
    private String slug;

    @Column(length = 500)
    private String excerpt;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(length = 120)
    private String author;

    @Column(length = 80)
    private String category;

    @Column(name = "tags", length = 300)
    private String tags;

    @Column(name = "image_url", length = 400)
    private String imageUrl;

    @Column(name = "read_minutes")
    private Integer readMinutes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "featured")
    private boolean featured;
}
