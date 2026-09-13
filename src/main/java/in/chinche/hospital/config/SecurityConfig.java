package in.chinche.hospital.config;

import in.chinche.hospital.security.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Security topology:
 *   - the entire public site + public booking API are anonymous-accessible;
 *   - /admin/** and /api/v1/admin/** require ROLE_ADMIN (form login, session);
 *   - actuator and API docs are locked down;
 *   - hardened response headers for a healthcare context.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                    // Public JSON endpoints are stateless and carry no privileged mutation
                    // without a session, so they are exempt; the admin console keeps CSRF on.
                    // NOTE: do NOT set requireCsrfProtectionMatcher here — replacing the
                    // default matcher drops its safe-method exemption, which would demand a
                    // token for GET /admin/login and 403 the login page itself.
                    .ignoringRequestMatchers("/api/v1/**", "/h2-console/**"))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/index", "/home").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/img/**", "/fonts/**", "/favicon.ico",
                                     "/site.webmanifest", "/robots.txt", "/sitemap.xml").permitAll()
                    .requestMatchers("/vendor/**", "/webjars/**").permitAll()
                    .requestMatchers("/api/v1/**").permitAll()
                    .requestMatchers("/book", "/book/**", "/track/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    .requestMatchers("/api-docs", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**").hasRole("ADMIN")
                    // Must precede the /admin/** rule: the login page and the logout POST
                    // are matched by that pattern, and denying them locks the console shut
                    // for anonymous users (403 on the very page used to authenticate).
                    .requestMatchers("/admin/login", "/admin/logout").permitAll()
                    .requestMatchers("/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                    .anyRequest().permitAll())
            .formLogin(form -> form
                    .loginPage("/admin/login")
                    .loginProcessingUrl("/admin/login")
                    .defaultSuccessUrl("/admin/dashboard", true)
                    .failureUrl("/admin/login?error")
                    .permitAll())
            .logout(logout -> logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll())
            .userDetailsService(userDetailsService)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            // contentTypeOptions and cacheControl defaults are already enabled by Spring Security.
            // Permissions-Policy is emitted by the reverse proxy / a filter instead — the
            // HeadersConfigurer#permissionsPolicy accessor is deprecated in 6.x and returns a
            // different builder type, which breaks fluent chaining.
            .headers(h -> h
                    .frameOptions(f -> f.sameOrigin())           // H2 console + map embeds
                    .referrerPolicy(r -> r.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .httpStrictTransportSecurity(t -> t
                            .maxAgeInSeconds(31536000)
                            .includeSubDomains(true)))
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
