package at.technikum.dms.messaging;

/**
 * Nachricht an den OCR-Worker, sobald ein Dokument gespeichert wurde (als JSON über RabbitMQ).
 * Dient gleichzeitig als Spring-Event, damit erst nach erfolgreichem Commit gesendet wird.
 * Später: z.B. Storage-Key (MinIO) ergänzen, damit der Worker die PDF laden kann.
 */
public record DocumentCreatedMessage(Long documentId, String title) {
}
