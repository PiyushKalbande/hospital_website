package in.chinche.hospital.api;

import in.chinche.hospital.dto.response.ApiResponse;
import in.chinche.hospital.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Content", description = "CMS-driven landing page content")
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentApiController {

    private final ContentService content;

    @Operation(summary = "Hero carousel slides")
    @GetMapping("/hero")
    public ApiResponse<?> hero() { return ApiResponse.ok(content.heroSlides()); }

    @Operation(summary = "Latest announcements")
    @GetMapping("/announcements")
    public ApiResponse<?> announcements() { return ApiResponse.ok(content.latestAnnouncements()); }

    @Operation(summary = "Latest health blogs")
    @GetMapping("/blogs")
    public ApiResponse<?> blogs() { return ApiResponse.ok(content.latestBlogs()); }

    @Operation(summary = "Featured patient stories")
    @GetMapping("/stories")
    public ApiResponse<?> stories() { return ApiResponse.ok(content.featuredStories()); }

    @Operation(summary = "Patient testimonials")
    @GetMapping("/testimonials")
    public ApiResponse<?> testimonials() { return ApiResponse.ok(content.testimonials()); }

    @Operation(summary = "Awards and accreditations")
    @GetMapping("/awards")
    public ApiResponse<?> awards() { return ApiResponse.ok(content.awards()); }

    @Operation(summary = "Service shortcut tiles")
    @GetMapping("/quick-links")
    public ApiResponse<?> quickLinks(@RequestParam(defaultValue = "HERO_SERVICES") String group) {
        return ApiResponse.ok(content.footerLinks(group));
    }
}
