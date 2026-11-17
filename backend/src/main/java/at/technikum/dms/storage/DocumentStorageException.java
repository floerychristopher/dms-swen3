package at.technikum.dms.storage;

public class DocumentStorageException extends RuntimeException {
    public DocumentStorageException(String message, Exception cause) {
        super(message, cause);
    }
}
