package at.technikum.dms.exception;

import lombok.Getter;

/** Wird geworfen, wenn ein eindeutiger Wert bereits vergeben ist → HTTP 409. */
@Getter
public class DuplicateResourceException extends RuntimeException {

    /** Name des betroffenen Feldes, damit die UI den Fehler am richtigen Eingabefeld anzeigen kann. */
    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }
}

