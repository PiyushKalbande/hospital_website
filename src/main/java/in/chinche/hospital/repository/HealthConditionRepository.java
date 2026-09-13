package in.chinche.hospital.repository;

import in.chinche.hospital.domain.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthConditionRepository extends JpaRepository<HealthCondition, Long> {

    Optional<HealthCondition> findBySlug(String slug);

    List<HealthCondition> findByActiveTrueOrderBySortOrderAsc();

    List<HealthCondition> findTop12ByActiveTrueOrderBySortOrderAsc();
}
