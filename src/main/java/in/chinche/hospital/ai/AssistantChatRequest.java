package in.chinche.hospital.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
        @NotBlank @Size(max = 500) String message,
        @Pattern(regexp = "en|mr", message = "Language must be en or mr.") String language
) {
}
