package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** A consultant on the panel. Public profile + scheduling root. */
@Entity
@Table(name = "doctor", indexes = {
        @Index(name = "idx_doctor_slug", columnList = "slug", unique = true),
        @Index(name = "idx_doctor_speciality", columnList = "speciality_id"),
        @Index(name = "idx_doctor_facility", columnList = "facility_id"),
        @Index(name = "idx_doctor_active", columnList = "active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Doctor extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 180, unique = true)
    private String slug;

    @Column(length = 120)
    private String designation;

    @Column(length = 200)
    private String qualifications;

    @Column(name = "registration_no", length = 60)
    private String registrationNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "speciality_id")
    private Speciality speciality;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(name = "experience_years")
    private Integer experienceYears;

    /** Comma-separated language codes, e.g. "mr,hi,en". */
    @Column(length = 120)
    private String languages;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "photo_url", length = 400)
    private String photoUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bio;

    /** Comma-separated focus areas rendered as chips on the profile. */
    @Column(name = "areas_of_expertise", length = 600)
    private String areasOfExpertise;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "teleconsult_enabled")
    private boolean teleconsultEnabled = true;

    /** Higher values surface first in "Find a doctor" results. */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** Pinned to the landing-page "Meet our specialists" band. */
    @Column(name = "featured")
    private boolean featured;

    @Column(nullable = false)
    private boolean active = true;

    /** Inverse side — never serialised; loaded explicitly by DoctorService. */
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<DoctorSchedule> schedules = new ArrayList<>();

    private static final java.util.Map<String, String> LANGUAGE_LABELS = java.util.Map.of(
            "mr", "मराठी", "hi", "हिंदी", "en", "English", "gu", "ગુજરાતી", "ta", "தமிழ்");

    /** Human-readable language list for templates; falls back to the trilingual default. */
    @Transient
    public String getLanguagesDisplay() {
        if (languages == null || languages.isBlank()) return "मराठी, हिंदी, English";
        return java.util.Arrays.stream(languages.split(","))
                .map(String::trim)
                .filter(c -> !c.isEmpty())
                .map(c -> LANGUAGE_LABELS.getOrDefault(c.toLowerCase(), c))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    @Transient
    public java.util.List<String> getExpertiseList() {
        if (areasOfExpertise == null || areasOfExpertise.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(areasOfExpertise.split(","))
                .map(String::trim).filter(c -> !c.isEmpty()).toList();
    }
}
