package at.technikum.dms.messaging;

import at.technikum.dms.config.RabbitMQConfig;
import at.technikum.dms.repository.DocumentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentMessagePublisher {
    private final RabbitTemplate rabbitTemplate;
    private final DocumentOutboxRepository outbox;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishNext() {
        outbox.findFirstByOrderByDocumentId().ifPresent(message -> {
            try {
                CorrelationData correlation = new CorrelationData();
                var payload = new OcrRequest(message.getDocumentId(), message.getTitle(), message.getStorageKey());
                rabbitTemplate.convertAndSend("", RabbitMQConfig.DOCUMENT_CREATED_QUEUE, payload, correlation);
                var confirmation = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirmation.ack() || correlation.getReturned() != null) {
                    throw new IllegalStateException("RabbitMQ did not accept or route the OCR request");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while sending OCR request for document {}", message.getDocumentId());
                return;
            } catch (Exception ex) {
                log.warn("Could not queue OCR request for document {}. Will retry.",
                        message.getDocumentId(), ex);
                return;
            }
            outbox.delete(message);
            log.info("Queued OCR request for document {}", message.getDocumentId());
        });
    }

    private record OcrRequest(Long documentId, String title, String storageKey) {}
}
