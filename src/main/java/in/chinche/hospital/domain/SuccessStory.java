package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Outcome story card: headline, treating doctor, patient origin, optional video. */
@Entity
@Table(name = "success_story", indexes = {
        @Index(name = "idx_story_slug", columnList = "slug", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuccessStory extends BaseEntity {

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 260, unique = true)
    private String slug;

    @Column(length = 600)
    private String summary;

    @Column(name = "patient_label", length = 120)
    private String patientLabel;

    @Column(name = "patient_city", length = 120)
    private String patientCity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "speciality_id")
    private Speciality speciality;

    /** YouTube id — rendered through a privacy-friendly facade, never an iframe by default. */
    @Column(name = "video_id", length = 40)
    private String videoId;

    @Column(name = "image_url", length = 400)
    private String imageUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "featured")
    private boolean featured;

    @Column(nullable = false)
    private boolean active = true;
}
