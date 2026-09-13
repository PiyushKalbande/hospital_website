package in.chinche.hospital.service;

import in.chinche.hospital.domain.Doctor;
import in.chinche.hospital.domain.DoctorSchedule;
import in.chinche.hospital.dto.request.DoctorSearchRequest;
import in.chinche.hospital.dto.response.DoctorDto;
import in.chinche.hospital.dto.response.PageDto;
import in.chinche.hospital.exception.ResourceNotFoundException;
import in.chinche.hospital.repository.DoctorRepository;
import in.chinche.hospital.repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;

    public PageDto<DoctorDto> search(DoctorSearchRequest req) {
        Sort sort = switch (req.sort() == null ? "" : req.sort()) {
            case "name" -> Sort.by("name").ascending();
            case "experience" -> Sort.by("experienceYears").descending();
            case "fee" -> Sort.by("consultationFee").ascending();
            case "rating" -> Sort.by("rating").descending();
            default -> Sort.by("sortOrder").ascending().and(Sort.by("name").ascending());
        };
        String q = (req.q() == null || req.q().isBlank()) ? null : req.q().trim();
        Page<Doctor> page = doctors.search(q, req.specialityId(), req.facilityId(),
                Boolean.TRUE.equals(req.teleconsultOnly()), PageRequest.of(req.page(), req.size(), sort));
        return PageDto.of(page, DoctorDto::from);
    }

    public List<Doctor> featured() {
        return doctors.findTop8ByActiveTrueAndFeaturedTrueOrderBySortOrderAsc();
    }

    /** Featured panel as DTOs — the mapping must stay inside the transaction. */
    public List<DoctorDto> featuredDtos() {
        return featured().stream().map(DoctorDto::from).toList();
    }

    public List<Doctor> all() {
        return doctors.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    public List<DoctorDto> allDtos() {
        return all().stream().map(DoctorDto::from).toList();
    }

    public Doctor bySlug(String slug) {
        return doctors.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Doctor", slug));
    }

    public Doctor byId(Long id) {
        return doctors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor", id));
    }

    public List<Doctor> bySpeciality(Long specialityId) {
        return doctors.findBySpecialityIdAndActiveTrueOrderBySortOrderAsc(specialityId);
    }

    /**
     * Speciality team as DTOs. Doctor cards in templates read {@code specialityName},
     * so every list feeding a card must be projected inside the transaction.
     */
    public List<DoctorDto> bySpecialityDtos(Long specialityId) {
        return bySpeciality(specialityId).stream().map(DoctorDto::from).toList();
    }

    public long count() {
        return doctors.countByActiveTrue();
    }

    /** Weekly timetable for a profile page, Monday-first. */
    public List<DoctorSchedule> weeklySchedule(Long doctorId) {
        return schedules.findByDoctorIdAndActiveTrue(doctorId).stream()
                .sorted(Comparator.comparing((DoctorSchedule s) -> s.getDayOfWeek() == DayOfWeek.SUNDAY ? 7 : s.getDayOfWeek().getValue())
                        .thenComparing(DoctorSchedule::getStartTime))
                .toList();
    }
}
