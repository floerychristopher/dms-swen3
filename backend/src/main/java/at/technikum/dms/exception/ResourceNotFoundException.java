package at.technikum.dms.exception;

/** Wird geworfen, wenn eine angefragte Ressource nicht existiert → HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super("%s with id %d not found".formatted(resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
