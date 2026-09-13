package in.chinche.hospital.repository;

import in.chinche.hospital.domain.Blog;
import in.chinche.hospital.domain.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    Optional<Blog> findBySlug(String slug);

    List<Blog> findTop3ByStatusOrderByPublishedAtDesc(ContentStatus status);

    Page<Blog> findByStatusOrderByPublishedAtDesc(ContentStatus status, Pageable pageable);

    Page<Blog> findByStatusAndCategoryOrderByPublishedAtDesc(ContentStatus status, String category, Pageable pageable);
}
