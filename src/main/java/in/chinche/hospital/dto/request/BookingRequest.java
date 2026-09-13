package in.chinche.hospital.dto.request;

import in.chinche.hospital.domain.enums.Gender;
import in.chinche.hospital.domain.enums.ServiceType;
import in.chinche.hospital.domain.enums.VisitMode;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload for step 5 of the booking widget (the final "Confirm" submit).
 *
 * <p>Deliberately flat so the widget can POST a single object. Server-side
 * validation is authoritative — the client mirrors these rules for UX only.</p>
 */
public record BookingRequest(

        @NotNull(message = "Please choose the type of care you need.")
        ServiceType serviceType,

        @NotNull(message = "Please choose how you would like to be seen.")
        VisitMode visitMode,

        Long specialityId,

        Long facilityId,

        /** Required for consultation-type bookings; null for package/lab bookings. */
        Long doctorId,

        /** Required when serviceType = HEALTH_CHECKUP. */
        Long healthPackageId,

        @NotNull(message = "Please pick an appointment date.")
        @FutureOrPresent(message = "The appointment date cannot be in the past.")
        LocalDate appointmentDate,

        @NotNull(message = "Please pick a time slot.")
        LocalTime slotStart,

        @NotBlank(message = "Please enter the patient's full name.")
        @Size(max = 160)
        String patientName,

        /** Indian 10-digit mobile number. */
        @NotBlank(message = "Please enter a mobile number.")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number.")
        String patientPhone,

        @Email(message = "Enter a valid email address.")
        @Size(max = 160)
        String patientEmail,

        Gender patientGender,

        @Min(value = 0, message = "Age must be 0 or more.")
        @Max(value = 120, message = "Please enter a valid age.")
        Integer patientAge,

        LocalDate patientDob,

        @Size(max = 300)
        String address,

        @Size(max = 120)
        String city,

        @Pattern(regexp = "^\\d{6}$", message = "PIN code must be 6 digits.")
        String pincode,

        @Size(max = 1000)
        String symptoms,

        @Size(max = 400)
        String allergies,

        boolean consentSms,
        boolean consentEmail,

        @Size(max = 30)
        String paymentMode
) {
}
