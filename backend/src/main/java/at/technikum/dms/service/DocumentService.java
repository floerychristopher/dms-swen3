package at.technikum.dms.service;

import at.technikum.dms.dto.DocumentRequest;
import at.technikum.dms.dto.DocumentResponse;
import at.technikum.dms.entity.Document;
import at.technikum.dms.entity.DocumentCollection;
import at.technikum.dms.exception.ResourceNotFoundException;
import at.technikum.dms.mapper.DocumentMapper;
import at.technikum.dms.repository.CollectionRepository;
import at.technikum.dms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id").descending());

    private final DocumentRepository documentRepository;
    private final CollectionRepository collectionRepository;
    private final DocumentMapper documentMapper;

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAllBy(NEWEST_FIRST).stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    public DocumentResponse getDocument(Long id) {
        return documentMapper.toResponse(findDocumentOrThrow(id));
    }

    @Transactional
    public DocumentResponse createDocument(DocumentRequest request) {
        // Später: MultipartFile (PDF) entgegennehmen und im Object-Storage ablegen
        Document document = documentMapper.toEntity(request);
        document.setCollection(resolveCollection(request.collectionId()));
        return documentMapper.toResponse(documentRepository.save(document));
    }

    @Transactional
    public DocumentResponse updateDocument(Long id, DocumentRequest request) {
        Document document = findDocumentOrThrow(id);
        documentMapper.updateEntity(document, request);
        document.setCollection(resolveCollection(request.collectionId()));
        return documentMapper.toResponse(documentRepository.save(document));
    }

    @Transactional
    public void deleteDocument(Long id) {
        Document document = findDocumentOrThrow(id);
        documentRepository.delete(document);
    }

    Document findDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    private DocumentCollection resolveCollection(Long collectionId) {
        if (collectionId == null) {
            return null;
        }
        return collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", collectionId));
    }
}