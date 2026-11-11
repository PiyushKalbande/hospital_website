package in.chinche.hospital.ai;

import java.util.List;

public record AssistantChatResponse(
        String answer,
        String language,
        boolean emergency,
        boolean verified,
        List<AssistantSource> sources,
        List<AssistantDoctor> doctors,
        String bookingUrl,
        String emergencyUrl,
        String emergencyPhone
) {
}
