package in.chinche.hospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalTime;

/** A single away-from-clinic interval inside a doctor's working day. */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorAttendanceBreak {

    @Column(name = "break_start", nullable = false)
    private LocalTime startTime;

    @Column(name = "break_end", nullable = false)
    private LocalTime endTime;
}
