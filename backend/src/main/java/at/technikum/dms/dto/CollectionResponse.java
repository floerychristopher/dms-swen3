package at.technikum.dms.dto;

import java.time.LocalDateTime;

/** Ausgabe-DTO einer Collection inkl. Anzahl zugeordneter Dokumente. */
public record CollectionResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        long documentCount
) {
}

