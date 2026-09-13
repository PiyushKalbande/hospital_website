package in.chinche.hospital.repository;

import in.chinche.hospital.domain.HealthPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthPackageRepository extends JpaRepository<HealthPackage, Long> {

    Optional<HealthPackage> findBySlug(String slug);

    List<HealthPackage> findByActiveTrueOrderBySortOrderAsc();

    List<HealthPackage> findTop4ByActiveTrueAndFeaturedTrueOrderBySortOrderAsc();
}
