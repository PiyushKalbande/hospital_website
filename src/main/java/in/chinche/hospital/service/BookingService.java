package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.domain.*;
import in.chinche.hospital.domain.enums.*;
import in.chinche.hospital.dto.request.BookingRequest;
import in.chinche.hospital.dto.response.BookingConfirmationDto;
import in.chinche.hospital.exception.ResourceNotFoundException;
import in.chinche.hospital.exception.SlotUnavailableException;
import in.chinche.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Writes bookings. This is the transactional heart of the platform.
 *
 * <p>Double-booking is prevented in two independent ways:</p>
 * <ol>
 *   <li>An availability re-check inside this transaction (friendly error, fast path).</li>
 *   <li>The unique constraint {@code uk_appt_slot} on (doctor, date, slot_start, visit_mode)
 *       — the database is the final arbiter under concurrent submits, and the resulting
 *       {@link DataIntegrityViolationException} is converted into the same friendly error.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final FacilityRepository facilities;
    private final SpecialityRepository specialities;
    private final HealthPackageRepository packages;
    private final SlotAvailabilityService availability;
    private final NotificationService notifications;
    private final AppProperties props;

    @Transactional
    public BookingConfirmationDto book(BookingRequest req) {
        validateRequest(req);

        VisitMode mode = req.visitMode() == null ? VisitMode.IN_HOSPITAL : req.visitMode();
        Doctor doctor = req.doctorId() == null ? null : doctors.findById(req.doctorId())
                .filter(Doctor::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", req.doctorId()));

        Facility facility = resolveFacility(req, doctor);
        Speciality speciality = resolveSpeciality(req, doctor);
        HealthPackage healthPackage = req.healthPackageId() == null ? null : packages.findById(req.healthPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Health package", req.healthPackageId()));

        LocalDate date = req.appointmentDate();
        LocalTime start = req.slotStart();
        LocalTime end = start.plusMinutes(resolveSlotMinutes(doctor));

        // ---- Fast path: is the slot still open? ----
        if (doctor != null && !availability.isSlotOpen(doctor.getId(), date, start, mode)) {
            throw new SlotUnavailableException(
                    "That slot was just taken. Please choose another time — availability updates in real time.");
        }
        if (date.isBefore(LocalDate.now()) || date.isAfter(availability.lastBookableDate())) {
            throw new IllegalArgumentException("Appointment date is outside the bookable window.");
        }

        Patient patient = upsertPatient(req);

        Appointment appointment = Appointment.builder()
                .referenceNo(nextReference())
                .patient(patient)
                .doctor(doctor)
                .facility(facility)
                .speciality(speciality)
                .healthPackage(healthPackage)
                .serviceType(req.serviceType())
                .visitMode(mode)
                .appointmentDate(date)
                .slotStart(start)
                .slotEnd(end)
                .status(AppointmentStatus.PENDING)
                .amountPayable(resolveAmount(req, doctor, healthPackage))
                .paymentMode(req.paymentMode() == null || req.paymentMode().isBlank() ? "PAY_AT_HOSPITAL" : req.paymentMode())
                .symptoms(req.symptoms())
                .source("WEBSITE")
                .build();

        if (req.allergies() != null && !req.allergies().isBlank()) {
            patient.setAllergies(req.allergies());
        }

        try {
            appointments.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            // Final arbiter: another request won the race for this exact slot.
            log.warn("Slot race lost for doctor={} date={} slot={}", req.doctorId(), date, start);
            throw new SlotUnavailableException(
                    "That slot was just taken. Please choose another time — availability updates in real time.");
        }

        log.info("Booking created ref={} doctor={} date={} slot={}", appointment.getReferenceNo(),
                req.doctorId(), date, start);

        // Fire-and-forget: never let a mail server hiccup fail a confirmed booking.
        notifications.sendBookingConfirmation(appointment);

        return BookingConfirmationDto.from(appointment);
    }

    @Transactional(readOnly = true)
    public Appointment findByReference(String reference) {
        return appointments.findByReferenceNo(normaliseReference(reference))
                .orElseThrow(() -> new ResourceNotFoundException("Booking", reference));
    }

    @Transactional
    public BookingConfirmationDto cancel(String reference, String reason) {
        Appointment a = appointments.findByReferenceNo(normaliseReference(reference))
                .orElseThrow(() -> new ResourceNotFoundException("Booking", reference));
        if (a.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("This booking is already cancelled.");
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        a.setCancellationReason(reason == null || reason.isBlank() ? "Cancelled by patient" : reason.trim());
        appointments.save(a);
        notifications.sendCancellation(a);
        return BookingConfirmationDto.from(a);
    }

    @Transactional(readOnly = true)
    public java.util.List<Appointment> bookingsForPhone(String phone) {
        return patients.findByPhone(phone)
                .map(p -> appointments.findByPatientIdOrderByAppointmentDateDesc(p.getId()))
                .orElse(java.util.List.of());
    }

    // ------------------------------------------------------------------ internals

    private void validateRequest(BookingRequest req) {
        boolean needsDoctor = switch (req.serviceType()) {
            case OPD_CONSULTATION, TELE_CONSULTATION, SECOND_OPINION, SURGERY_PLANNING -> true;
            case HEALTH_CHECKUP, LAB_TEST, HOME_CARE -> false;
        };
        if (needsDoctor && req.doctorId() == null) {
            throw new IllegalArgumentException("Please choose a doctor for this type of appointment.");
        }
        if (!needsDoctor && req.doctorId() == null && req.facilityId() == null) {
            throw new IllegalArgumentException("Please choose a centre for this booking.");
        }
        if (req.serviceType() == ServiceType.HEALTH_CHECKUP && req.healthPackageId() == null) {
            throw new IllegalArgumentException("Please choose a health check-up package.");
        }
        if (req.serviceType() == ServiceType.TELE_CONSULTATION && req.visitMode() != VisitMode.VIDEO) {
            throw new IllegalArgumentException("Video consultations must use the video visit mode.");
        }
    }

    private Facility resolveFacility(BookingRequest req, Doctor doctor) {
        if (req.facilityId() != null) {
            return facilities.findById(req.facilityId()).orElse(null);
        }
        if (doctor != null && doctor.getFacility() != null) return doctor.getFacility();
        return facilities.findFirstByHeadquarterTrue().orElse(null);
    }

    private Speciality resolveSpeciality(BookingRequest req, Doctor doctor) {
        if (req.specialityId() != null) return specialities.findById(req.specialityId()).orElse(null);
        if (doctor != null && doctor.getSpeciality() != null) return doctor.getSpeciality();
        return null;
    }

    private int resolveSlotMinutes(Doctor doctor) {
        int configured = props.getBooking().getSlotMinutes();
        if (doctor == null) return configured;
        return doctor.getSchedules().stream()
                .map(DoctorSchedule::getSlotMinutes)
                .filter(m -> m != null && m > 0)
                .findFirst()
                .orElse(configured);
    }

    private BigDecimal resolveAmount(BookingRequest req, Doctor doctor, HealthPackage pkg) {
        if (pkg != null) return pkg.getPrice();
        if (doctor != null && doctor.getConsultationFee() != null) return doctor.getConsultationFee();
        return switch (req.serviceType()) {
            case LAB_TEST -> new BigDecimal("300.00");
            case HOME_CARE -> new BigDecimal("900.00");
            default -> BigDecimal.ZERO;
        };
    }

    private Patient upsertPatient(BookingRequest req) {
        Patient patient = patients.findByPhone(req.patientPhone()).orElseGet(() -> Patient.builder()
                .phone(req.patientPhone())
                .build());

        patient.setFullName(req.patientName().trim());
        if (req.patientEmail() != null && !req.patientEmail().isBlank()) patient.setEmail(req.patientEmail().trim());
        patient.setGender(req.patientGender());
        patient.setDateOfBirth(req.patientDob());
        patient.setAge(req.patientAge());
        if (req.address() != null) patient.setAddress(req.address());
        if (req.city() != null) patient.setCity(req.city());
        if (req.pincode() != null && req.pincode().matches("\\d{6}")) patient.setPincode(req.pincode());
        patient.setConsentSms(req.consentSms());
        patient.setConsentEmail(req.consentEmail());
        if (req.symptoms() != null) patient.setMedicalNotes(req.symptoms());
        return patients.save(patient);
    }

    /** CHH-2026-000184 — year-scoped, collision-checked, with a random tail as backstop. */
    private String nextReference() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = props.getBooking().getReferencePrefix() + "-" + year + "-";
        for (int attempt = 0; attempt < 8; attempt++) {
            long seq = appointments.count() + 1 + attempt;
            String candidate = prefix + String.format("%06d", seq);
            if (appointments.findByReferenceNo(candidate).isEmpty()) return candidate;
        }
        return prefix + String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999_999));
    }

    private String normaliseReference(String reference) {
        if (reference == null) return "";
        String trimmed = reference.trim().toUpperCase(Locale.ROOT);
        return trimmed.matches("^\\d+$")
                ? props.getBooking().getReferencePrefix() + "-" + Year.now().getValue() + "-"
                        + String.format("%06d", Long.parseLong(trimmed))
                : trimmed;
    }

}
