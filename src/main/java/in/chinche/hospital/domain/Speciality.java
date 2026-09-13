package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A clinical department (Cardiology, Orthopaedics, ...). Specialities drive the
 * tabbed catalogue grid on the landing page and the doctor-search facets.
 */
@Entity
@Table(name = "speciality", indexes = {
        @Index(name = "idx_speciality_slug", columnList = "slug", unique = true),
        @Index(name = "idx_speciality_active", columnList = "active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Speciality extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    /** Devanagari label used when the visitor switches to मराठी. */
    @Column(name = "name_mr", length = 120)
    private String nameMr;

    @Column(nullable = false, length = 140, unique = true)
    private String slug;

    /** Key into the local SVG sprite (see static/img/icons/sprite.svg). */
    @Column(name = "icon_key", length = 60)
    private String iconKey;

    @Column(name = "accent_color", length = 9)
    private String accentColor;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "featured")
    private boolean featured;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Inverse side — never serialised; loaded explicitly by CatalogService. */
    @OneToMany(mappedBy = "speciality", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Procedure> procedures = new ArrayList<>();

    @OneToMany(mappedBy = "speciality")
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Doctor> doctors = new ArrayList<>();
}
