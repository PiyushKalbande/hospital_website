package in.chinche.hospital.api;

import in.chinche.hospital.ai.AssistantChatRequest;
import in.chinche.hospital.ai.AssistantChatResponse;
import in.chinche.hospital.ai.HospitalAssistantService;
import in.chinche.hospital.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantApiController {

    private final HospitalAssistantService assistant;

    @PostMapping("/chat")
    public ApiResponse<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean staffAccess = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_RECEPTIONIST"));
        return ApiResponse.ok(assistant.answer(request, staffAccess));
    }
}
