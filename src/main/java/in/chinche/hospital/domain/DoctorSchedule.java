package in.chinche.hospital.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Recurring weekly availability window for a doctor at a facility.
 * Concrete bookable slots are derived from these windows by
 * {@code SlotAvailabilityService} (window / slotMinutes, minus already-booked).
 */
@Entity
@Table(name = "doctor_schedule", indexes = {
        @Index(name = "idx_schedule_doctor_day", columnList = "doctor_id,day_of_week"),
        @Index(name = "idx_schedule_facility", columnList = "facility_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_schedule", columnNames = {"doctor_id", "day_of_week", "start_time"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 12, nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Override of the global default (chinche.booking.slot-minutes). */
    @Column(name = "slot_minutes")
    private Integer slotMinutes;

    @Column(name = "max_patients_per_slot")
    private Integer maxPatientsPerSlot;

    /** Teleconsult slots are drawn from a separate availability pool. */
    @Column(name = "teleconsult")
    private boolean teleconsult;

    @Column(nullable = false)
    private boolean active = true;
}
