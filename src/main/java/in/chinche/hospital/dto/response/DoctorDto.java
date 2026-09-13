package in.chinche.hospital.dto.response;

import in.chinche.hospital.domain.Doctor;

import java.math.BigDecimal;
import java.util.List;

public record DoctorDto(
        Long id,
        String name,
        String slug,
        String designation,
        String qualifications,
        String registrationNo,
        Long specialityId,
        String specialityName,
        String specialitySlug,
        Long facilityId,
        String facilityName,
        String facilityCity,
        Integer experienceYears,
        List<String> languages,
        BigDecimal consultationFee,
        String photoUrl,
        String bio,
        List<String> areasOfExpertise,
        Double rating,
        Integer reviewCount,
        boolean teleconsultEnabled
) {
    public static DoctorDto from(Doctor d) {
        return new DoctorDto(
                d.getId(), d.getName(), d.getSlug(), d.getDesignation(), d.getQualifications(),
                d.getRegistrationNo(),
                d.getSpeciality() == null ? null : d.getSpeciality().getId(),
                d.getSpeciality() == null ? null : d.getSpeciality().getName(),
                d.getSpeciality() == null ? null : d.getSpeciality().getSlug(),
                d.getFacility() == null ? null : d.getFacility().getId(),
                d.getFacility() == null ? null : d.getFacility().getName(),
                d.getFacility() == null ? null : d.getFacility().getCity(),
                d.getExperienceYears(),
                splitCsv(d.getLanguages()),
                d.getConsultationFee(),
                d.getPhotoUrl(),
                d.getBio(),
                splitCsv(d.getAreasOfExpertise()),
                d.getRating(), d.getReviewCount(), d.isTeleconsultEnabled());
    }

    static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
