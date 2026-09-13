package in.chinche.hospital.api;

import in.chinche.hospital.domain.enums.VisitMode;
import in.chinche.hospital.dto.response.ApiResponse;
import in.chinche.hospital.dto.response.DayAvailabilityDto;
import in.chinche.hospital.service.SlotAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Powers the calendar strip and the slot grid in steps 4 of the booking widget.
 * Read-only and cheap, so the client can call it as often as the user changes dates.
 */
@Tag(name = "Availability", description = "Real-time slot availability")
@Validated
@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class SlotApiController {

    private final SlotAvailabilityService availability;

    @Operation(summary = "Bookable days and slots for a doctor")
    @GetMapping("/doctors/{doctorId}")
    public ApiResponse<List<DayAvailabilityDto>> forDoctor(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "14") @Min(1) @Max(60) int days,
            @RequestParam(defaultValue = "IN_HOSPITAL") VisitMode visitMode) {
        return ApiResponse.ok(availability.availability(doctorId, from, days, visitMode));
    }

    @Operation(summary = "Availability for one specific date")
    @GetMapping("/doctors/{doctorId}/date/{date}")
    public ApiResponse<DayAvailabilityDto> forDate(
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "IN_HOSPITAL") VisitMode visitMode) {
        return ApiResponse.ok(availability.availabilityForDate(doctorId, date, visitMode));
    }

    @Operation(summary = "Calendar boundaries exposed to the widget")
    @GetMapping("/window")
    public ApiResponse<Map<String, Object>> window() {
        return ApiResponse.ok(Map.of(
                "today", availability.today().toString(),
                "lastBookable", availability.lastBookableDate().toString()));
    }
}
