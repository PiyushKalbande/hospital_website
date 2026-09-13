package in.chinche.hospital.repository;

import in.chinche.hospital.domain.LabTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabTestRepository extends JpaRepository<LabTest, Long> {

    Optional<LabTest> findBySlug(String slug);

    List<LabTest> findByActiveTrueOrderByNameAsc();

    List<LabTest> findByCategoryAndActiveTrueOrderByNameAsc(String category);
}
