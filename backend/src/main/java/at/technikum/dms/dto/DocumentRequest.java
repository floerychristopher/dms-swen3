package at.technikum.dms.dto;

import at.technikum.dms.entity.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Eingabe-DTO zum Anlegen/Aktualisieren eines Dokuments.
 * Nur diese Felder kann der Client setzen (kein Over-Posting von id/createdAt).
 */
public record DocumentRequest(
        @NotBlank(message = "Title is required")
        @Size(max = Document.TITLE_MAX_LENGTH, message = "Title must be at most {max} characters")
        String title,

        @Size(max = Document.DESCRIPTION_MAX_LENGTH, message = "Description must be at most {max} characters")
        String description,

        @Positive(message = "Collection id must be positive")
        Long collectionId
) {
}

