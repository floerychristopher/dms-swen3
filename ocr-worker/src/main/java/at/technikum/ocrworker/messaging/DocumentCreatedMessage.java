package at.technikum.ocrworker.messaging;

/** Gegenstück zur Nachricht im REST-Server (gleiche Feldnamen → gleiches JSON). */
public record DocumentCreatedMessage(Long documentId, String title) {
}
