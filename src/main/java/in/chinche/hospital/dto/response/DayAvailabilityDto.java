package in.chinche.hospital.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Availability for one calendar day, grouped into day-parts by the client. */
public record DayAvailabilityDto(
        LocalDate date,
        String dayName,
        boolean clinicOpen,
        String reason,
        List<SlotDto> slots
) {
}
