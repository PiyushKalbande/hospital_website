package in.chinche.hospital.repository;

import in.chinche.hospital.domain.Announcement;
import in.chinche.hospital.domain.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Optional<Announcement> findBySlug(String slug);

    List<Announcement> findTop4ByStatusOrderByPinnedDescPublishedAtDesc(ContentStatus status);

    Page<Announcement> findByStatusOrderByPinnedDescPublishedAtDesc(ContentStatus status, Pageable pageable);
}
