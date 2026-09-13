package in.chinche.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * चींचे हॉस्पिटल | Chinche Hospital — Katol, Nagpur, Maharashtra.
 *
 * <p>Entry point for the hospital digital-care platform. The application is a hybrid:
 * server-rendered Thymeleaf pages for SEO-critical public content, plus a versioned
 * JSON REST API ({@code /api/v1}) that powers the multi-step booking widget and the
 * administration console.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class ChincheHospitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChincheHospitalApplication.class, args);
    }
}
