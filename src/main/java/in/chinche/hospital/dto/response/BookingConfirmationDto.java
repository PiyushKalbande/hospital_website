package in.chinche.hospital.dto.response;

import in.chinche.hospital.domain.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** What the widget renders on the success screen (step 6). */
public record BookingConfirmationDto(
        String referenceNo,
        String status,
        String patientName,
        String serviceTypeLabel,
        String visitModeLabel,
        String doctorName,
        String doctorDesignation,
        String specialityName,
        String facilityName,
        String facilityAddress,
        String facilityPhone,
        LocalDate appointmentDate,
        LocalTime slotStart,
        LocalTime slotEnd,
        BigDecimal amountPayable,
        String paymentMode,
        String preparationNote,
        String trackingUrl
) {
    public static BookingConfirmationDto from(Appointment a) {
        return new BookingConfirmationDto(
                a.getReferenceNo(),
                a.getStatus().getLabel(),
                a.getPatient().getFullName(),
                a.getServiceType().getLabel(),
                a.getVisitMode().getLabel(),
                a.getDoctor() == null ? null : a.getDoctor().getName(),
                a.getDoctor() == null ? null : a.getDoctor().getDesignation(),
                a.getSpeciality() == null ? null : a.getSpeciality().getName(),
                a.getFacility() == null ? null : a.getFacility().getName(),
                a.getFacility() == null ? null : a.getFacility().getAddressLine(),
                a.getFacility() == null ? null : a.getFacility().getPhone(),
                a.getAppointmentDate(), a.getSlotStart(), a.getSlotEnd(),
                a.getAmountPayable(), a.getPaymentMode(),
                buildPreparationNote(a),
                "/track/" + a.getReferenceNo());
    }

    private static String buildPreparationNote(Appointment a) {
        return switch (a.getServiceType()) {
            case HEALTH_CHECKUP -> "Please arrive fasting (no food for 10–12 hours; water is fine). Carry any previous reports.";
            case LAB_TEST -> a.getHealthPackage() != null && a.getHealthPackage().isFastingRequired()
                    ? "Fasting sample: please keep a 10–12 hour fasting gap before your visit."
                    : "No fasting required. Carry a government photo ID and previous prescriptions if any.";
            case TELE_CONSULTATION -> "A secure video link will be sent by SMS and email 15 minutes before your slot. Keep your reports handy.";
            case SECOND_OPINION -> "Please upload or carry all previous investigation reports, discharge summaries and imaging films.";
            case HOME_CARE -> "Our care coordinator will call you to confirm the visit address and timing.";
            case SURGERY_PLANNING -> "Please bring all imaging (X-ray/MRI/CT) films and reports, plus a list of current medications.";
            case OPD_CONSULTATION -> "Please arrive 15 minutes early with your ID and previous prescriptions.";
        };
    }
}
