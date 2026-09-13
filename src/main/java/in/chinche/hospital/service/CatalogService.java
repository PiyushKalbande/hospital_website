package in.chinche.hospital.service;

import in.chinche.hospital.config.CacheConfig;
import in.chinche.hospital.domain.*;
import in.chinche.hospital.dto.response.HealthPackageDto;
import in.chinche.hospital.dto.response.SpecialityDto;
import in.chinche.hospital.exception.ResourceNotFoundException;
import in.chinche.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-model for the clinical catalogue: specialities, procedures, conditions, packages, lab tests. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final SpecialityRepository specialities;
    private final ProcedureRepository procedures;
    private final DoctorRepository doctors;
    private final HealthConditionRepository conditions;
    private final HealthPackageRepository packages;
    private final LabTestRepository labTests;

    public List<Speciality> allSpecialities() {
        return specialities.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Cacheable(value = CacheConfig.SPECIALITIES, key = "'featured'", unless = "#result == null or #result.isEmpty()")
    public List<Speciality> featuredSpecialities() {
        List<Speciality> featured = specialities.findTop12ByActiveTrueAndFeaturedTrueOrderBySortOrderAsc();
        return featured.isEmpty() ? specialities.findTop12ByActiveTrueOrderBySortOrderAsc() : featured;
    }

    @Cacheable(value = CacheConfig.SPECIALITIES, unless = "#result == null or #result.isEmpty()")
    public List<SpecialityDto> allSpecialityDtos() {
        return specialities.findByActiveTrueOrderBySortOrderAscNameAsc()
                .stream().map(SpecialityDto::lite).toList();
    }

    /** Procedures belonging to a speciality — resolved inside the transaction (inverse side is lazy). */
    @Transactional(readOnly = true)
    public List<Procedure> proceduresForSpeciality(String specialitySlug) {
        Speciality s = specialityBySlug(specialitySlug);
        return procedures.findBySpecialityIdAndActiveTrueOrderBySortOrderAsc(s.getId());
    }

    /** Speciality as a DTO with a live panel count — resolved inside the transaction. */
    @Transactional(readOnly = true)
    public SpecialityDto specialityDtoBySlug(String slug) {
        Speciality s = specialityBySlug(slug);
        return SpecialityDto.of(s, doctors.countBySpecialityIdAndActiveTrue(s.getId()));
    }

    public Speciality specialityBySlug(String slug) {
        return specialities.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Speciality", slug));
    }

    public Speciality specialityById(Long id) {
        return specialities.findById(id).orElseThrow(() -> new ResourceNotFoundException("Speciality", id));
    }

    public List<Procedure> allProcedures() {
        return procedures.findByActiveTrueOrderBySortOrderAsc();
    }

    public List<Procedure> topProcedures() {
        List<Procedure> all = allProcedures();
        return all.size() > 10 ? all.subList(0, 10) : all;
    }

    public Procedure procedureBySlug(String slug) {
        return procedures.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Procedure", slug));
    }

    public List<HealthCondition> allConditions() {
        return conditions.findByActiveTrueOrderBySortOrderAsc();
    }

    public List<HealthCondition> topConditions() {
        List<HealthCondition> all = allConditions();
        return all.size() > 12 ? all.subList(0, 12) : all;
    }

    public HealthCondition conditionBySlug(String slug) {
        return conditions.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Condition", slug));
    }

    public List<HealthPackage> allPackages() {
        return packages.findByActiveTrueOrderBySortOrderAsc();
    }

    @Cacheable(value = CacheConfig.PACKAGES, unless = "#result == null or #result.isEmpty()")
    public List<HealthPackageDto> allPackageDtos() {
        return packages.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(HealthPackageDto::from).toList();
    }

    @Cacheable(value = CacheConfig.PROCEDURES, unless = "#result == null or #result.isEmpty()")
    public List<in.chinche.hospital.dto.response.ProcedureDto> allProcedureDtos() {
        return procedures.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(in.chinche.hospital.dto.response.ProcedureDto::from).toList();
    }

    @Cacheable(value = CacheConfig.CONDITIONS, unless = "#result == null or #result.isEmpty()")
    public List<in.chinche.hospital.dto.response.ConditionDto> allConditionDtos() {
        return conditions.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(in.chinche.hospital.dto.response.ConditionDto::from).toList();
    }

    @Cacheable(value = CacheConfig.LAB_TESTS, unless = "#result == null or #result.isEmpty()")
    public List<in.chinche.hospital.dto.response.LabTestDto> allLabTestDtos() {
        return labTests.findByActiveTrueOrderByNameAsc()
                .stream().map(in.chinche.hospital.dto.response.LabTestDto::from).toList();
    }

    public List<HealthPackage> featuredPackages() {
        return packages.findTop4ByActiveTrueAndFeaturedTrueOrderBySortOrderAsc();
    }

    public HealthPackage packageBySlug(String slug) {
        return packages.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Health package", slug));
    }

    public HealthPackage packageById(Long id) {
        return packages.findById(id).orElseThrow(() -> new ResourceNotFoundException("Health package", id));
    }

    public List<LabTest> allLabTests() {
        return labTests.findByActiveTrueOrderByNameAsc();
    }

    /** Any catalogue write must drop every catalogue cache — the bands are read as a set. */
    @CacheEvict(value = {CacheConfig.SPECIALITIES, CacheConfig.PROCEDURES, CacheConfig.CONDITIONS,
            CacheConfig.PACKAGES, CacheConfig.LAB_TESTS, CacheConfig.DOCTOR_SEARCH}, allEntries = true)
    public void evictCatalogueCaches() {
        // intentional no-op: annotation drives the eviction
    }
}
