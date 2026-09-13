package in.chinche.hospital.api;

import in.chinche.hospital.dto.response.ApiResponse;
import in.chinche.hospital.service.AdminStatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin", description = "Dashboard aggregates — staff only")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
public class AdminApiController {

    private final AdminStatsService stats;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(stats.overview());
    }

    @GetMapping("/stats/trend")
    public ApiResponse<?> trend(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.ok(stats.dailyTrend(Math.min(days, 90)));
    }

    @GetMapping("/stats/specialities")
    public ApiResponse<?> bySpeciality() {
        return ApiResponse.ok(stats.bySpeciality());
    }
}
