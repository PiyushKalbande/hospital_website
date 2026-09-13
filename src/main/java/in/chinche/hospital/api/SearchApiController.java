package in.chinche.hospital.api;

import in.chinche.hospital.dto.response.ApiResponse;
import in.chinche.hospital.dto.response.SearchSuggestionDto;
import in.chinche.hospital.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Search", description = "Unified omni-search suggestions")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchApiController {

    private final SearchService searchService;

    @Operation(summary = "Type-ahead suggestions across doctors, specialities, procedures, conditions and packages")
    @GetMapping("/suggest")
    public ApiResponse<List<SearchSuggestionDto>> suggest(@RequestParam String q,
                                                          @RequestParam(defaultValue = "12") int limit) {
        return ApiResponse.ok(searchService.suggest(q, Math.min(limit, 30)));
    }
}
