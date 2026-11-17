package at.technikum.dms.exception;

public class InvalidPdfException extends RuntimeException {
    public InvalidPdfException() {
        super("The file must start with a PDF header");
    }
}
