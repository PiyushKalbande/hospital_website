package in.chinche.hospital.api;

import in.chinche.hospital.dto.response.*;
import in.chinche.hospital.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Catalogue", description = "Specialities, procedures, conditions, health packages and lab tests")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogApiController {

    private final CatalogService catalogue;

    @Operation(summary = "All active specialities")
    @GetMapping("/specialities")
    public ApiResponse<List<SpecialityDto>> specialities() {
        return ApiResponse.ok(catalogue.allSpecialityDtos());
    }

    @Operation(summary = "Speciality by slug")
    @GetMapping("/specialities/{slug}")
    public ApiResponse<SpecialityDto> speciality(@PathVariable String slug) {
        return ApiResponse.ok(catalogue.specialityDtoBySlug(slug));
    }

    @Operation(summary = "All active procedures")
    @GetMapping("/procedures")
    public ApiResponse<List<ProcedureDto>> procedures() {
        return ApiResponse.ok(catalogue.allProcedureDtos());
    }

    @Operation(summary = "All treatable conditions")
    @GetMapping("/conditions")
    public ApiResponse<List<ConditionDto>> conditions() {
        return ApiResponse.ok(catalogue.allConditionDtos());
    }

    @Operation(summary = "All health check-up packages")
    @GetMapping("/health-packages")
    public ApiResponse<List<HealthPackageDto>> packages() {
        return ApiResponse.ok(catalogue.allPackageDtos());
    }

    @Operation(summary = "Health package by slug")
    @GetMapping("/health-packages/{slug}")
    public ApiResponse<HealthPackageDto> packageBySlug(@PathVariable String slug) {
        return ApiResponse.ok(HealthPackageDto.from(catalogue.packageBySlug(slug)));
    }

    @Operation(summary = "All bookable lab tests")
    @GetMapping("/lab-tests")
    public ApiResponse<List<LabTestDto>> labTests() {
        return ApiResponse.ok(catalogue.allLabTestDtos());
    }
}
