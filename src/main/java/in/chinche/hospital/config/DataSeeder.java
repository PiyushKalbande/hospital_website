package in.chinche.hospital.config;

import in.chinche.hospital.service.seeding.DemoDataSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Runs the demo-catalogue seeder once on an empty database. */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final AppProperties props;
    private final DemoDataSeeder seeder;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner bootstrapData() {
        return args -> {
            if (!props.getSeeding().isEnabled()) {
                log.info("Demo seeding disabled (chinche.seeding.enabled=false)");
                return;
            }
            seeder.seedIfEmpty(passwordEncoder);
        };
    }
}
