package in.chinche.hospital.dto.response;

import in.chinche.hospital.domain.LabTest;

import java.math.BigDecimal;
import java.util.List;

/** Flat, serialisation-safe view of a bookable lab test. */
public record LabTestDto(
        Long id,
        String name,
        String nameMr,
        String slug,
        String category,
        BigDecimal price,
        String sampleType,
        Integer reportHours,
        boolean fastingRequired,
        boolean homeCollection,
        List<String> parameters
) {
    public static LabTestDto from(LabTest t) {
        return new LabTestDto(
                t.getId(), t.getName(), t.getNameMr(), t.getSlug(), t.getCategory(), t.getPrice(),
                t.getSampleType(), t.getReportHours(), t.isFastingRequired(), t.isHomeCollection(),
                DoctorDto.splitCsv(t.getParameters()));
    }
}
