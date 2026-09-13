package in.chinche.hospital.api;

import in.chinche.hospital.dto.response.ApiResponse;
import in.chinche.hospital.dto.response.FacilityDto;
import in.chinche.hospital.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Facilities", description = "Hospitals, clinics and diagnostic centres")
@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class FacilityApiController {

    private final FacilityService facilities;

    @Operation(summary = "All active facilities")
    @GetMapping
    public ApiResponse<List<FacilityDto>> all() {
        return ApiResponse.ok(facilities.allDtos());
    }

    @Operation(summary = "Facility by slug")
    @GetMapping("/{slug}")
    public ApiResponse<FacilityDto> bySlug(@PathVariable String slug) {
        return ApiResponse.ok(FacilityDto.from(facilities.bySlug(slug)));
    }
}
