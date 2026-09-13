package in.chinche.hospital.repository;

import in.chinche.hospital.domain.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    Optional<Facility> findBySlug(String slug);

    List<Facility> findByActiveTrueOrderBySortOrderAsc();

    Optional<Facility> findFirstByHeadquarterTrue();

    List<Facility> findByCityContainingIgnoreCaseAndActiveTrue(String city);
}
