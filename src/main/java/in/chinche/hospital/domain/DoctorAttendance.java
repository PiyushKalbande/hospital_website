package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Daily attendance ledger maintained by hospital administrators. */
@Entity
@Table(name = "doctor_attendance", indexes = {
        @Index(name = "idx_attendance_date", columnList = "attendance_date"),
        @Index(name = "idx_attendance_doctor_date", columnList = "doctor_id,attendance_date", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorAttendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "doctor_attendance_break", joinColumns = @JoinColumn(name = "attendance_id"))
    @OrderColumn(name = "break_order")
    @Builder.Default
    private List<DoctorAttendanceBreak> breaks = new ArrayList<>();

    @Column(length = 500)
    private String notes;
}
