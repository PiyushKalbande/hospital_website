package in.chinche.hospital.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Read-heavy catalogue data (specialities, packages, content bands) is cached in
 * process. The abstraction is a Spring {@link CacheManager}, so swapping in Redis
 * later for a multi-node deployment is a dependency + config change only.
 */
@Configuration
public class CacheConfig {

    public static final String SPECIALITIES = "specialities";
    public static final String PROCEDURES = "procedures";
    public static final String CONDITIONS = "conditions";
    public static final String FACILITIES = "facilities";
    public static final String PACKAGES = "packages";
    public static final String LAB_TESTS = "labTests";
    public static final String HOME_BANNER = "homeBanner";
    public static final String CONTENT = "content";
    public static final String DOCTOR_SEARCH = "doctorSearch";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        List<Cache> caches = List.of(SPECIALITIES, PROCEDURES, CONDITIONS, FACILITIES, PACKAGES,
                        LAB_TESTS, HOME_BANNER, CONTENT, DOCTOR_SEARCH)
                .stream()
                .map(ConcurrentMapCache::new)
                .map(Cache.class::cast)
                .toList();
        manager.setCaches(caches);
        return manager;
    }
}
