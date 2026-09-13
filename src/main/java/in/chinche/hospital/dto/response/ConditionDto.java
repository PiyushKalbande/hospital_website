package in.chinche.hospital.dto.response;

import in.chinche.hospital.domain.HealthCondition;

/** Flat, serialisation-safe view of a treatable condition. */
public record ConditionDto(
        Long id,
        String name,
        String nameMr,
        String slug,
        String iconKey,
        Integer sortOrder,
        Long specialityId,
        String specialityName,
        String specialitySlug
) {
    public static ConditionDto from(HealthCondition c) {
        return new ConditionDto(
                c.getId(), c.getName(), c.getNameMr(), c.getSlug(), c.getIconKey(), c.getSortOrder(),
                c.getSpeciality() == null ? null : c.getSpeciality().getId(),
                c.getSpeciality() == null ? null : c.getSpeciality().getName(),
                c.getSpeciality() == null ? null : c.getSpeciality().getSlug());
    }
}
