package in.chinche.hospital.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Resolves the active language on every request.
 *
 * <p>Precedence: {@code ?lang=} parameter → session → default (English). The chosen
 * locale is also exposed to templates through the {@code lang} request attribute so
 * the switcher can highlight the active language and {@code <html lang>}} can be set
 * correctly for screen readers and SEO.</p>
 */
@Component
@RequiredArgsConstructor
public class LocaleInterceptor implements HandlerInterceptor {

    private static final List<String> SUPPORTED = List.of("en", "mr");

    private final LocaleResolver localeResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requested = request.getParameter("lang");
        if (requested != null && SUPPORTED.contains(requested.toLowerCase())) {
            localeResolver.setLocale(request, response, Locale.forLanguageTag(requested));
        }
        Locale current = localeResolver.resolveLocale(request);
        request.setAttribute("lang", current.getLanguage());
        request.setAttribute("isMarathi", "mr".equals(current.getLanguage()));
        LocaleContextHolder.setLocale(current);
        return true;
    }
}
