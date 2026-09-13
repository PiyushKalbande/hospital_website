package in.chinche.hospital.domain;

import in.chinche.hospital.domain.enums.AppointmentStatus;
import in.chinche.hospital.domain.enums.ServiceType;
import in.chinche.hospital.domain.enums.VisitMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The booking aggregate — the output of the multi-step widget.
 *
 * <p>The unique constraint on (doctor, date, slot_start, visit_mode) is the
 * last line of defence against double-booking: even under concurrent submits
 * the database rejects the second insert, which the service translates into a
 * friendly "this slot was just taken" response.</p>
 */
@Entity
@Table(name = "appointment",
        indexes = {
                @Index(name = "idx_appt_reference", columnList = "reference_no", unique = true),
                @Index(name = "idx_appt_doctor_date", columnList = "doctor_id,appointment_date"),
                @Index(name = "idx_appt_patient", columnList = "patient_id"),
                @Index(name = "idx_appt_status", columnList = "status"),
                @Index(name = "idx_appt_date", columnList = "appointment_date")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_appt_slot",
                columnNames = {"doctor_id", "appointment_date", "slot_start", "visit_mode"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment extends BaseEntity {

    /** Human-readable booking reference, e.g. CHH-2026-000184. */
    @Column(name = "reference_no", nullable = false, length = 32, unique = true)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "speciality_id")
    private Speciality speciality;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "health_package_id")
    private HealthPackage healthPackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 30, nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_mode", length = 20, nullable = false)
    private VisitMode visitMode = VisitMode.IN_HOSPITAL;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "slot_start", nullable = false)
    private LocalTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalTime slotEnd;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "amount_payable", precision = 10, scale = 2)
    private BigDecimal amountPayable;

    @Column(name = "payment_mode", length = 30)
    private String paymentMode;

    @Column(length = 1000)
    private String symptoms;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "cancellation_reason", length = 400)
    private String cancellationReason;

    @Column(name = "source", length = 30)
    private String source = "WEBSITE";

    @Column(name = "reminder_sent")
    private boolean reminderSent;

    @Transient
    public String getSlotDisplay() {
        return slotStart + " – " + slotEnd;
    }
}
