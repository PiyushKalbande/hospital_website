package in.chinche.hospital.dto.response;

import in.chinche.hospital.domain.Facility;

import java.util.List;

public record FacilityDto(
        Long id, String name, String nameMr, String slug, String facilityType,
        String city, String district, String state, String pincode, String addressLine,
        String phone, String emergencyPhone, String email,
        Double latitude, Double longitude, Double rating, Integer reviewCount,
        Integer bedCount, String imageUrl, List<String> amenities, String hoursText,
        boolean headquarter
) {
    public static FacilityDto from(Facility f) {
        return new FacilityDto(f.getId(), f.getName(), f.getNameMr(), f.getSlug(),
                f.getFacilityType() == null ? null : f.getFacilityType().name(),
                f.getCity(), f.getDistrict(), f.getState(), f.getPincode(), f.getAddressLine(),
                f.getPhone(), f.getEmergencyPhone(), f.getEmail(), f.getLatitude(), f.getLongitude(),
                f.getRating(), f.getReviewCount(), f.getBedCount(), f.getImageUrl(),
                DoctorDto.splitCsv(f.getAmenities()), f.getHoursText(), f.isHeadquarter());
    }
}
