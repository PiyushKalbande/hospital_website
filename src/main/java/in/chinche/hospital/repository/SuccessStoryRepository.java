package in.chinche.hospital.repository;

import in.chinche.hospital.domain.SuccessStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuccessStoryRepository extends JpaRepository<SuccessStory, Long> {

    Optional<SuccessStory> findBySlug(String slug);

    List<SuccessStory> findByActiveTrueOrderByPublishedAtDesc();

    List<SuccessStory> findTop6ByActiveTrueAndFeaturedTrueOrderByPublishedAtDesc();
}
