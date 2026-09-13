package in.chinche.hospital.api;

import in.chinche.hospital.dto.request.EnquiryRequest;
import in.chinche.hospital.dto.response.ApiResponse;
import in.chinche.hospital.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Enquiries", description = "Contact forms and callback requests")
@RestController
@RequestMapping("/api/v1/enquiries")
@RequiredArgsConstructor
public class EnquiryApiController {

    private final EnquiryService enquiries;

    @Operation(summary = "Submit a contact or callback request")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> submit(@Valid @RequestBody EnquiryRequest request) {
        var saved = enquiries.submit(request);
        return ApiResponse.ok("धन्यवाद! Our care team will contact you within one working day.",
                Map.of("id", saved.getId(), "status", saved.getStatus()));
    }
}
