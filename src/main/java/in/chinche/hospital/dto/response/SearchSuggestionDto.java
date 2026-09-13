package in.chinche.hospital.dto.response;

/** One row in the omni-search autocomplete (doctors, specialities, procedures, conditions). */
public record SearchSuggestionDto(
        String type,      // DOCTOR | SPECIALITY | PROCEDURE | CONDITION | PACKAGE
        String id,
        String label,
        String sublabel,
        String url
) {
}
