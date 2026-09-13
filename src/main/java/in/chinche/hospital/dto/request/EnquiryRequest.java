package in.chinche.hospital.dto.request;

import jakarta.validation.constraints.*;

public record EnquiryRequest(
        @NotBlank(message = "Please tell us your name.")
        @Size(max = 160)
        String name,

        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number.")
        String phone,

        @Email(message = "Enter a valid email address.")
        @Size(max = 160)
        String email,

        @Size(max = 30)
        String enquiryType,

        @Size(max = 200)
        String subject,

        @NotBlank(message = "Please write a short message.")
        @Size(max = 4000)
        String message,

        Long facilityId,

        @Size(max = 200)
        String sourcePage
) {
}
