package in.chinche.hospital.api;

import in.chinche.hospital.dto.request.DoctorSearchRequest;
import in.chinche.hospital.dto.response.*;
import in.chinche.hospital.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Doctors", description = "Doctor discovery and profiles")
@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorApiController {

    private final DoctorService doctors;

    @Operation(summary = "Faceted doctor search")
    @GetMapping
    public ApiResponse<PageDto<DoctorDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long specialityId,
            @RequestParam(required = false) Long facilityId,
            @RequestParam(required = false) Boolean teleconsultOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "sortOrder") String sort) {
        return ApiResponse.ok(doctors.search(new DoctorSearchRequest(q, specialityId, facilityId,
                teleconsultOnly, page, size, sort)));
    }

    @Operation(summary = "Doctor profile by slug")
    @GetMapping("/{slug}")
    public ApiResponse<DoctorDto> bySlug(@PathVariable String slug) {
        return ApiResponse.ok(DoctorDto.from(doctors.bySlug(slug)));
    }

    @Operation(summary = "Featured doctors for the landing page")
    @GetMapping("/featured")
    public ApiResponse<List<DoctorDto>> featured() {
        return ApiResponse.ok(doctors.featuredDtos());
    }
}
