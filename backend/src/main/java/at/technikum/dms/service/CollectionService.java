package at.technikum.dms.service;

import at.technikum.dms.dto.CollectionRequest;
import at.technikum.dms.dto.CollectionResponse;
import at.technikum.dms.dto.DocumentResponse;
import at.technikum.dms.entity.Document;
import at.technikum.dms.entity.DocumentCollection;
import at.technikum.dms.exception.DuplicateResourceException;
import at.technikum.dms.exception.ResourceNotFoundException;
import at.technikum.dms.mapper.CollectionMapper;
import at.technikum.dms.mapper.DocumentMapper;
import at.technikum.dms.repository.CollectionRepository;
import at.technikum.dms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Zusätzlicher Use-Case: Dokumente in Collections organisieren
 * (Collections verwalten, Dokumente zuordnen/entfernen, Inhalt einer Collection auflisten).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private static final Sort BY_NAME = Sort.by(Sort.Order.asc("name").ignoreCase());

    private final CollectionRepository collectionRepository;
    private final DocumentRepository documentRepository;
    private final CollectionMapper collectionMapper;
    private final DocumentMapper documentMapper;

    public List<CollectionResponse> getAllCollections() {
        return collectionRepository.findAll(BY_NAME).stream()
                .map(this::toResponse)
                .toList();
    }

    public CollectionResponse getCollection(Long id) {
        return toResponse(findCollectionOrThrow(id));
    }

    @Transactional
    public CollectionResponse createCollection(CollectionRequest request) {
        DocumentCollection collection = collectionMapper.toEntity(request);
        if (collectionRepository.existsByNameIgnoreCase(collection.getName())) {
            throw duplicateName(collection.getName());
        }
        return collectionMapper.toResponse(collectionRepository.save(collection), 0);
    }

    @Transactional
    public CollectionResponse updateCollection(Long id, CollectionRequest request) {
        DocumentCollection collection = findCollectionOrThrow(id);
        // Erst prüfen, dann die (managed) Entity ändern: sonst flusht Hibernate den neuen Namen
        // vor der Query und der Unique-Constraint wirft statt einer sauberen 409-Antwort mit Feldfehler.
        String name = request.name().strip();
        if (collectionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName(name);
        }
        collectionMapper.updateEntity(collection, request);
        return toResponse(collectionRepository.save(collection));
    }

    /** Löscht die Collection. Enthaltene Dokumente bleiben erhalten, verlieren aber ihre Zuordnung. */
    @Transactional
    public void deleteCollection(Long id) {
        DocumentCollection collection = findCollectionOrThrow(id);
        documentRepository.unassignAllFromCollection(id);
        collectionRepository.delete(collection);
    }

    public List<DocumentResponse> getDocumentsOfCollection(Long collectionId) {
        findCollectionOrThrow(collectionId);
        return documentRepository.findAllByCollectionId(collectionId, DocumentService.NEWEST_FIRST).stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    @Transactional
    public DocumentResponse assignDocument(Long collectionId, Long documentId) {
        DocumentCollection collection = findCollectionOrThrow(collectionId);
        Document document = findDocumentOrThrow(documentId);
        document.setCollection(collection);
        return documentMapper.toResponse(documentRepository.save(document));
    }

    @Transactional
    public DocumentResponse removeDocument(Long collectionId, Long documentId) {
        findCollectionOrThrow(collectionId);
        Document document = findDocumentOrThrow(documentId);
        if (document.getCollection() == null || !collectionId.equals(document.getCollection().getId())) {
            throw new ResourceNotFoundException(
                    "Document with id %d is not part of collection %d".formatted(documentId, collectionId));
        }
        document.setCollection(null);
        return documentMapper.toResponse(documentRepository.save(document));
    }

    private CollectionResponse toResponse(DocumentCollection collection) {
        return collectionMapper.toResponse(collection, documentRepository.countByCollectionId(collection.getId()));
    }

    private DocumentCollection findCollectionOrThrow(Long id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));
    }

    private Document findDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    private static DuplicateResourceException duplicateName(String name) {
        return new DuplicateResourceException("name", "A collection named '%s' already exists".formatted(name));
    }
}