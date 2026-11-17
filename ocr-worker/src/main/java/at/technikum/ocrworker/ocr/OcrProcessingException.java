package at.technikum.ocrworker.ocr;

public class OcrProcessingException extends RuntimeException {
    public OcrProcessingException(String message, Exception cause) {
        super(message, cause);
    }
}
