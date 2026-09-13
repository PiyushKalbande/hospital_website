package in.chinche.hospital.dto.response;

import java.time.LocalTime;

/** One bookable (or unavailable) time slot rendered by the widget. */
public record SlotDto(
        LocalTime start,
        LocalTime end,
        boolean available,
        String period   // MORNING | AFTERNOON | EVENING
) {
    public static String periodOf(LocalTime t) {
        int h = t.getHour();
        if (h < 12) return "MORNING";
        if (h < 17) return "AFTERNOON";
        return "EVENING";
    }
}
