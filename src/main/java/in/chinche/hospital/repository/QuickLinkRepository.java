package in.chinche.hospital.repository;

import in.chinche.hospital.domain.QuickLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuickLinkRepository extends JpaRepository<QuickLink, Long> {
    List<QuickLink> findByActiveTrueAndLinkGroupOrderBySortOrderAsc(String linkGroup);
}
