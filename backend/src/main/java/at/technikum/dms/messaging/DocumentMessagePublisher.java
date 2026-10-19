package at.technikum.dms.messaging;

import at.technikum.dms.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Schickt Dokument-Events an RabbitMQ – aber erst NACH erfolgreichem Datenbank-Commit.
 * So bekommt der OCR-Worker nie eine Nachricht zu einem Dokument, das gar nicht gespeichert wurde.
 */
@Component
@RequiredArgsConstructor
public class DocumentMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentCreated(DocumentCreatedMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.DOCUMENT_CREATED_QUEUE, message);
    }
}
