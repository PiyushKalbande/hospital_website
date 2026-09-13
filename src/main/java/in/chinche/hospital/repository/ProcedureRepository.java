package in.chinche.hospital.repository;

import in.chinche.hospital.domain.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, Long> {

    Optional<Procedure> findBySlug(String slug);

    List<Procedure> findByActiveTrueOrderBySortOrderAsc();

    List<Procedure> findBySpecialityIdAndActiveTrueOrderBySortOrderAsc(Long specialityId);

    List<Procedure> findTop10ByActiveTrueOrderBySortOrderAsc();
}
