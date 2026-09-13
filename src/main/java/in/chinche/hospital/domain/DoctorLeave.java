package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Blackout dates (leave, conference, OT day) that suppress slot generation. */
@Entity
@Table(name = "doctor_leave", indexes = {
        @Index(name = "idx_leave_doctor_date", columnList = "doctor_id,leave_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorLeave extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    @Column(length = 200)
    private String reason;
}
