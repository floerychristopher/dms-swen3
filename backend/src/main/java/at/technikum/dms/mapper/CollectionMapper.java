package at.technikum.dms.mapper;

import at.technikum.dms.dto.CollectionRequest;
import at.technikum.dms.dto.CollectionResponse;
import at.technikum.dms.entity.DocumentCollection;
import org.springframework.stereotype.Component;

/** Wandelt zwischen DocumentCollection-Entity und DTOs um. */
@Component
public class CollectionMapper {

    public CollectionResponse toResponse(DocumentCollection collection, long documentCount) {
        return new CollectionResponse(
                collection.getId(),
                collection.getName(),
                collection.getDescription(),
                collection.getCreatedAt(),
                documentCount
        );
    }

    public DocumentCollection toEntity(CollectionRequest request) {
        DocumentCollection collection = new DocumentCollection();
        updateEntity(collection, request);
        return collection;
    }

    public void updateEntity(DocumentCollection collection, CollectionRequest request) {
        collection.setName(MapperUtils.trim(request.name()));
        collection.setDescription(MapperUtils.trimToNull(request.description()));
    }
}

