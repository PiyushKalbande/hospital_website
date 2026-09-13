package in.chinche.hospital.dto.response;

import in.chinche.hospital.domain.HealthPackage;

import java.math.BigDecimal;
import java.util.List;

public record HealthPackageDto(
        Long id, String name, String nameMr, String slug, String tier,
        BigDecimal price, BigDecimal listPrice, String tagline, String taglineMr,
        List<String> includes, Integer testCount, String description, String descriptionMr,
        String genderSuitability, Integer minAge, Integer maxAge,
        boolean fastingRequired, Integer reportHours, boolean homeCollection,
        String iconKey, boolean featured
) {
    public static HealthPackageDto from(HealthPackage p) {
        return new HealthPackageDto(p.getId(), p.getName(), p.getNameMr(), p.getSlug(), p.getTier(),
                p.getPrice(), p.getListPrice(), p.getTagline(), p.getTaglineMr(),
                splitLines(p.getIncludes()), p.getTestCount(), p.getDescription(), p.getDescriptionMr(),
                p.getGenderSuitability(), p.getMinAge(), p.getMaxAge(), p.isFastingRequired(),
                p.getReportHours(), p.isHomeCollection(), p.getIconKey(), p.isFeatured());
    }

    static List<String> splitLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        return java.util.Arrays.stream(text.split("\\R")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
