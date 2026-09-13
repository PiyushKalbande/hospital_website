package in.chinche.hospital.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a slug or id does not resolve to a live record.
 *
 * <p>Annotated with 404 so browser traffic lands on the branded error page via
 * {@code ErrorPageController}. The REST advice still catches it first for
 * {@code /api/v1/**} callers and returns the JSON envelope.</p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object key) {
        super(resource + " not found: " + key);
    }
}
