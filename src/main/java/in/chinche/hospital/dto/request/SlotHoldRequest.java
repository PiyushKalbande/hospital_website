package in.chinche.hospital.dto.request;

import in.chinche.hospital.domain.enums.VisitMode;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Optional soft-lock call issued by the widget while the patient fills step 5. */
public record SlotHoldRequest(
        @NotNull Long doctorId,
        @NotNull LocalDate appointmentDate,
        @NotNull LocalTime slotStart,
        VisitMode visitMode,
        Integer holdSeconds
) {
}
