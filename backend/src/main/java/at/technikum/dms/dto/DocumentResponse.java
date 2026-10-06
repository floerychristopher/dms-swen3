package at.technikum.dms.dto;

import java.time.LocalDateTime;

/** Ausgabe-DTO eines Dokuments (inkl. Name der zugeordneten Collection). */
public record DocumentResponse(
        Long id,
        String title,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long collectionId,
        String collectionName
) {
}

