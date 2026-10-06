package at.technikum.dms.dto;

import at.technikum.dms.entity.DocumentCollection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Eingabe-DTO zum Anlegen/Aktualisieren einer Collection. */
public record CollectionRequest(
        @NotBlank(message = "Name is required")
        @Size(max = DocumentCollection.NAME_MAX_LENGTH, message = "Name must be at most {max} characters")
        String name,

        @Size(max = DocumentCollection.DESCRIPTION_MAX_LENGTH, message = "Description must be at most {max} characters")
        String description
) {
}

