package at.technikum.ocrworker.messaging;

import at.technikum.ocrworker.config.RabbitMQConfig;
import at.technikum.ocrworker.ocr.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
public class OcrListener {

    //Infos überProgrammablauf ausgeben
    private static final Logger log = LoggerFactory.getLogger(OcrListener.class);
    private final OcrService ocrService;

    public OcrListener(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    //Sobald Nachricht in Queue, ruft Spring Methode auf
    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
    public void onDocumentCreated(DocumentCreatedMessage message) {
        if (message.storageKey() == null || message.storageKey().isBlank()) {
            log.info("Skipping OCR for document {} because it has no PDF", message.documentId());
            return;
        }
        log.info("Received document {} for OCR processing (storage key: {})",
                message.documentId(), message.storageKey());
        ocrService.process(message.documentId(), message.storageKey());
    }
}
