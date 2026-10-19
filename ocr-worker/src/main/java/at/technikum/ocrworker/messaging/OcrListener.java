package at.technikum.ocrworker.messaging;

import at.technikum.ocrworker.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * "Leerer" OCR-Worker: empfängt die Nachrichten des REST-Servers, macht aber noch keine Texterkennung.
 * Später: PDF aus dem Storage (MinIO) laden, OCR (z.B. Tesseract) ausführen, Ergebnis zurückmelden.
 */
@Component
public class OcrListener {

    private static final Logger log = LoggerFactory.getLogger(OcrListener.class);

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
    public void onDocumentCreated(DocumentCreatedMessage message) {
        log.info("Received document {} ('{}') for OCR processing", message.documentId(), message.title());
    }
}
