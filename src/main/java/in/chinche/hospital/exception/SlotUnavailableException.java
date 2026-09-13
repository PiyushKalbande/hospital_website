package in.chinche.hospital.exception;

/** Raised when a slot is taken between the availability call and the confirm submit. */
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}
