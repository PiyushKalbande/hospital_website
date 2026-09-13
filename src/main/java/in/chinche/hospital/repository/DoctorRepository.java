package in.chinche.hospital.repository;

import in.chinche.hospital.domain.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findBySlug(String slug);

    List<Doctor> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Doctor> findTop8ByActiveTrueAndFeaturedTrueOrderBySortOrderAsc();

    List<Doctor> findBySpecialityIdAndActiveTrueOrderBySortOrderAsc(Long specialityId);

    List<Doctor> findByFacilityIdAndActiveTrueOrderBySortOrderAsc(Long facilityId);

    long countByActiveTrue();

    /** Panel size per department — avoids initialising Speciality.doctors just to count it. */
    long countBySpecialityIdAndActiveTrue(Long specialityId);

    /**
     * Faceted doctor search backing "Find a Doctor". Every parameter is optional —
     * null means "don't filter on this facet". Name matching spans the English and
     * Devanagari columns plus qualification text.
     */
    @Query("""
            select distinct d from Doctor d
            left join fetch d.speciality s
            left join fetch d.facility f
            where d.active = true
              and (:q is null or lower(d.name) like lower(concat('%', :q, '%'))
                               or lower(d.designation) like lower(concat('%', :q, '%'))
                               or lower(d.qualifications) like lower(concat('%', :q, '%'))
                               or lower(s.name) like lower(concat('%', :q, '%')))
              and (:specialityId is null or s.id = :specialityId)
              and (:facilityId is null or f.id = :facilityId)
              and (:teleconsultOnly = false or d.teleconsultEnabled = true)
            """)
    Page<Doctor> search(@Param("q") String q,
                        @Param("specialityId") Long specialityId,
                        @Param("facilityId") Long facilityId,
                        @Param("teleconsultOnly") boolean teleconsultOnly,
                        Pageable pageable);
}
