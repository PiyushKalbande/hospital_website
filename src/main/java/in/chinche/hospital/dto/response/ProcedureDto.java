package in.chinche.hospital.dto.response;

import in.chinche.hospital.domain.Procedure;

/** Flat, serialisation-safe view of a procedure — no lazy proxies reach Jackson. */
public record ProcedureDto(
        Long id,
        String name,
        String nameMr,
        String slug,
        String iconKey,
        String summary,
        Integer indicativeCost,
        Integer typicalStayDays,
        Integer sortOrder,
        Long specialityId,
        String specialityName,
        String specialitySlug
) {
    public static ProcedureDto from(Procedure p) {
        return new ProcedureDto(
                p.getId(), p.getName(), p.getNameMr(), p.getSlug(), p.getIconKey(), p.getSummary(),
                p.getIndicativeCost(), p.getTypicalStayDays(), p.getSortOrder(),
                p.getSpeciality() == null ? null : p.getSpeciality().getId(),
                p.getSpeciality() == null ? null : p.getSpeciality().getName(),
                p.getSpeciality() == null ? null : p.getSpeciality().getSlug());
    }
}
