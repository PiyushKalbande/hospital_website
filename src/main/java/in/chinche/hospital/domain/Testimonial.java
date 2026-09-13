package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

/** Patient review shown in the trust band. */
@Entity
@Table(name = "testimonial")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Testimonial extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String patientName;

    @Column(length = 120)
    private String city;

    @Column(length = 200)
    private String relation;

    @Column(name = "rating")
    private Integer rating;

    @Column(length = 1000)
    private String quote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "speciality_id")
    private Speciality speciality;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
