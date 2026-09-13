package in.chinche.hospital.repository;

import in.chinche.hospital.domain.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialityRepository extends JpaRepository<Speciality, Long> {

    Optional<Speciality> findBySlug(String slug);

    List<Speciality> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Speciality> findTop12ByActiveTrueAndFeaturedTrueOrderBySortOrderAsc();

    /** Fallback when no speciality is flagged featured — keeps the home grid populated. */
    List<Speciality> findTop12ByActiveTrueOrderBySortOrderAsc();

    @Query("select s from Speciality s where s.active = true and "
            + "(lower(s.name) like lower(concat('%', :q, '%')) or lower(s.nameMr) like concat('%', :q, '%'))")
    List<Speciality> search(String q);
}
