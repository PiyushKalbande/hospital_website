package in.chinche.hospital.ai;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AssistantUnavailableException extends ResponseStatusException {

    public AssistantUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "The hospital assistant is currently unavailable.");
    }
}
