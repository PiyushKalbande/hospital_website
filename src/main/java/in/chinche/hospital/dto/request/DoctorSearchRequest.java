package in.chinche.hospital.dto.request;

/** Query parameters for the "Find a Doctor" facet search. */
public record DoctorSearchRequest(
        String q,
        Long specialityId,
        Long facilityId,
        Boolean teleconsultOnly,
        Integer page,
        Integer size,
        String sort
) {
    public DoctorSearchRequest {
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 12 : Math.min(size, 60);
        teleconsultOnly = Boolean.TRUE.equals(teleconsultOnly);
        sort = (sort == null || sort.isBlank()) ? "sortOrder" : sort;
    }
}
