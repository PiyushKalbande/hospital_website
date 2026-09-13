package in.chinche.hospital.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Bilingual plumbing for the EN / मराठी toggle.
 *
 * <p>Kept separate from {@link WebConfig} on purpose: {@code WebConfig} injects the
 * {@code LocaleInterceptor}, and the interceptor injects the {@code LocaleResolver}.
 * Declaring the resolver in the same class that consumes the interceptor creates a
 * bean cycle, which Spring Boot 3 refuses to start.</p>
 */
@Configuration
public class I18nConfig {

    /** Session-backed locale so the language choice persists across pages. */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasenames("classpath:i18n/messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        ms.setUseCodeAsDefaultMessage(true);
        ms.setCacheSeconds(300);
        return ms;
    }
}
