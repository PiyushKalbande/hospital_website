package in.chinche.hospital.dto.response;

import in.chinche.hospital.domain.Speciality;

public record SpecialityDto(
        Long id, String name, String nameMr, String slug, String iconKey,
        String accentColor, String summary, boolean featured, long doctorCount
) {
    /**
     * Projection with a caller-supplied panel size.
     *
     * <p>Never read {@code Speciality.getDoctors()} to derive this: the collection is
     * lazy, and with {@code open-in-view=false} touching it outside a transaction
     * throws LazyInitializationException (which Jackson surfaces as a 500).</p>
     */
    public static SpecialityDto of(Speciality s, long doctorCount) {
        return new SpecialityDto(s.getId(), s.getName(), s.getNameMr(), s.getSlug(), s.getIconKey(),
                s.getAccentColor(), s.getSummary(), s.isFeatured(), doctorCount);
    }

    /** Panel size unknown or irrelevant — reports 0 rather than touching the collection. */
    public static SpecialityDto lite(Speciality s) {
        return of(s, 0);
    }
}
