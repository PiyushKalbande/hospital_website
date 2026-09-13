package in.chinche.hospital.service;

import in.chinche.hospital.config.CacheConfig;
import in.chinche.hospital.domain.*;
import in.chinche.hospital.domain.enums.ContentStatus;
import in.chinche.hospital.exception.ResourceNotFoundException;
import in.chinche.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Everything CMS-like that decorates the landing page and content sections. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final HeroSlideRepository heroSlides;
    private final AnnouncementRepository announcements;
    private final BlogRepository blogs;
    private final SuccessStoryRepository stories;
    private final TestimonialRepository testimonials;
    private final AwardRepository awards;
    private final QuickLinkRepository quickLinks;

    @Cacheable(value = CacheConfig.HOME_BANNER, unless = "#result == null or #result.isEmpty()")
    public List<HeroSlide> heroSlides() {
        return heroSlides.findByActiveTrueOrderBySortOrderAsc();
    }

    @Cacheable(value = CacheConfig.CONTENT, key = "'announcements'", unless = "#result == null or #result.isEmpty()")
    public List<Announcement> latestAnnouncements() {
        return announcements.findTop4ByStatusOrderByPinnedDescPublishedAtDesc(ContentStatus.PUBLISHED);
    }

    public Page<Announcement> announcements(int page, int size) {
        return announcements.findByStatusOrderByPinnedDescPublishedAtDesc(
                ContentStatus.PUBLISHED, PageRequest.of(page, Math.min(size, 50)));
    }

    public Announcement announcementBySlug(String slug) {
        return announcements.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Announcement", slug));
    }

    @Cacheable(value = CacheConfig.CONTENT, key = "'blogs'", unless = "#result == null or #result.isEmpty()")
    public List<Blog> latestBlogs() {
        return blogs.findTop3ByStatusOrderByPublishedAtDesc(ContentStatus.PUBLISHED);
    }

    public Page<Blog> blogs(int page, int size, String category) {
        PageRequest pr = PageRequest.of(page, Math.min(size, 50));
        return (category == null || category.isBlank())
                ? blogs.findByStatusOrderByPublishedAtDesc(ContentStatus.PUBLISHED, pr)
                : blogs.findByStatusAndCategoryOrderByPublishedAtDesc(ContentStatus.PUBLISHED, category, pr);
    }

    public Blog blogBySlug(String slug) {
        return blogs.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Blog", slug));
    }

    @Cacheable(value = CacheConfig.CONTENT, key = "'stories'", unless = "#result == null or #result.isEmpty()")
    public List<SuccessStory> featuredStories() {
        List<SuccessStory> featured = stories.findTop6ByActiveTrueAndFeaturedTrueOrderByPublishedAtDesc();
        return featured.isEmpty() ? stories.findByActiveTrueOrderByPublishedAtDesc() : featured;
    }

    public List<SuccessStory> allStories() {
        return stories.findByActiveTrueOrderByPublishedAtDesc();
    }

    @Cacheable(value = CacheConfig.CONTENT, key = "'testimonials'", unless = "#result == null or #result.isEmpty()")
    public List<Testimonial> testimonials() {
        return testimonials.findByActiveTrueOrderBySortOrderAsc();
    }

    @Cacheable(value = CacheConfig.CONTENT, key = "'awards'", unless = "#result == null or #result.isEmpty()")
    public List<Award> awards() {
        return awards.findByActiveTrueOrderBySortOrderAsc();
    }

    @Cacheable(value = CacheConfig.CONTENT, key = "'quicklinks-hero'", unless = "#result == null or #result.isEmpty()")
    public List<QuickLink> heroQuickLinks() {
        return quickLinks.findByActiveTrueAndLinkGroupOrderBySortOrderAsc("HERO_SERVICES");
    }

    @Cacheable(value = CacheConfig.CONTENT, key = "'quicklinks-patient'", unless = "#result == null or #result.isEmpty()")
    public List<QuickLink> patientCornerLinks() {
        return quickLinks.findByActiveTrueAndLinkGroupOrderBySortOrderAsc("PATIENT_CORNER");
    }

    public List<QuickLink> footerLinks(String group) {
        return quickLinks.findByActiveTrueAndLinkGroupOrderBySortOrderAsc(group);
    }
}
