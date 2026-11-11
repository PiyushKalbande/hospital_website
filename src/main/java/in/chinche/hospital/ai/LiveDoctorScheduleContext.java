package in.chinche.hospital.ai;

import java.util.List;

public record LiveDoctorScheduleContext(
        String context,
        String fallbackAnswer,
        AssistantDoctor doctor,
        List<AssistantSource> sources,
        String bookingUrl,
        boolean verified
) {
}
