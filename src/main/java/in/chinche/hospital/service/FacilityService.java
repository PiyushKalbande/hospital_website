package in.chinche.hospital.service;

import in.chinche.hospital.config.AppProperties;
import in.chinche.hospital.config.CacheConfig;
import in.chinche.hospital.domain.Facility;
import in.chinche.hospital.dto.response.FacilityDto;
import in.chinche.hospital.exception.ResourceNotFoundException;
import in.chinche.hospital.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilities;
    private final AppProperties props;

    @Cacheable(value = CacheConfig.FACILITIES, unless = "#result == null or #result.isEmpty()")
    public List<Facility> all() {
        return facilities.findByActiveTrueOrderBySortOrderAsc();
    }

    public List<FacilityDto> allDtos() {
        return all().stream().map(FacilityDto::from).toList();
    }

    public Facility bySlug(String slug) {
        return facilities.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Facility", slug));
    }

    public Facility byId(Long id) {
        return facilities.findById(id).orElseThrow(() -> new ResourceNotFoundException("Facility", id));
    }

    /** Default facility for bookings that do not specify one — the Katol headquarters. */
    public Facility primary() {
        return facilities.findFirstByHeadquarterTrue().orElseGet(() -> all().stream().findFirst().orElse(null));
    }

    public AppProperties.Contact contact() {
        return props.getContact();
    }
}
