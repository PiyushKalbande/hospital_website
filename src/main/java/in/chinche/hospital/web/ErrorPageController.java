package in.chinche.hospital.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/** Replaces the Spring Boot whitelabel error page with on-brand pages. */
@Slf4j
@Controller
public class ErrorPageController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = status == null ? 500 : Integer.parseInt(status.toString());
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        model.addAttribute("errorCode", code);
        model.addAttribute("errorPath", path);
        model.addAttribute("errorTitle", switch (code) {
            case 404 -> "We couldn't find that page";
            case 403 -> "You don't have access to this area";
            case 405 -> "That action isn't allowed here";
            case 500 -> "Something went wrong on our side";
            default -> "Unexpected error";
        });
        model.addAttribute("errorBody", switch (code) {
            case 404 -> "The link may be outdated or the page may have moved. Try the search above, or head back to the home page.";
            case 403 -> "This area is restricted to hospital staff. If you believe you should have access, contact the IT desk.";
            default -> "Our team has been notified. If you were in the middle of booking, your progress is safe — please try again.";
        });
        log.warn("Rendering error page: {} for {}", code, path);
        return "error/error";
    }
}
