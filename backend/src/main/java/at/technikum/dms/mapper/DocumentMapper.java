package at.technikum.dms.mapper;

import at.technikum.dms.dto.DocumentRequest;
import at.technikum.dms.dto.DocumentResponse;
import at.technikum.dms.entity.Document;
import at.technikum.dms.entity.DocumentCollection;
import org.springframework.stereotype.Component;

/** Wandelt zwischen Document-Entity und DTOs um. */
@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document document) {
        DocumentCollection collection = document.getCollection();
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                collection != null ? collection.getId() : null,
                collection != null ? collection.getName() : null
        );
    }

    /** Erzeugt eine neue (noch nicht persistierte) Entity. Die Collection setzt der Service. */
    public Document toEntity(DocumentRequest request) {
        Document document = new Document();
        updateEntity(document, request);
        return document;
    }

    /** Übernimmt die editierbaren Felder aus dem Request in eine bestehende Entity. */
    public void updateEntity(Document document, DocumentRequest request) {
        document.setTitle(MapperUtils.trim(request.title()));
        document.setDescription(MapperUtils.trimToNull(request.description()));
    }
}

