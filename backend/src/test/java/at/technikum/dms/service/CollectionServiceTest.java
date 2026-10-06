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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Spy
    private CollectionMapper collectionMapper = new CollectionMapper();

    @Spy
    private DocumentMapper documentMapper = new DocumentMapper();

    @InjectMocks
    private CollectionService collectionService;

    private static DocumentCollection collection(Long id, String name) {
        return DocumentCollection.builder().id(id).name(name).build();
    }

    private static Document document(Long id, DocumentCollection collection) {
        Document doc = new Document("Doc " + id, null);
        doc.setId(id);
        doc.setCollection(collection);
        return doc;
    }

    @Test
    void getAllCollections_includesDocumentCount() {
        when(collectionRepository.findAll(any(Sort.class))).thenReturn(List.of(collection(1L, "Invoices")));
        when(documentRepository.countByCollectionId(1L)).thenReturn(4L);

        List<CollectionResponse> result = collectionService.getAllCollections();

        assertThat(result).singleElement().satisfies(c -> {
            assertThat(c.name()).isEqualTo("Invoices");
            assertThat(c.documentCount()).isEqualTo(4L);
        });
    }

    @Test
    void createCollection_savesTrimmedName() {
        when(collectionRepository.existsByNameIgnoreCase("Invoices")).thenReturn(false);
        when(collectionRepository.save(any(DocumentCollection.class))).thenAnswer(inv -> {
            DocumentCollection c = inv.getArgument(0);
            c.setId(7L);
            return c;
        });

        CollectionResponse result = collectionService.createCollection(new CollectionRequest("  Invoices ", ""));

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("Invoices");
        assertThat(result.description()).isNull();
        assertThat(result.documentCount()).isZero();
    }

    @Test
    void createCollection_duplicateName_throwsConflict() {
        when(collectionRepository.existsByNameIgnoreCase("Invoices")).thenReturn(true);

        assertThatThrownBy(() -> collectionService.createCollection(new CollectionRequest("Invoices", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("field").isEqualTo("name");
        verify(collectionRepository, never()).save(any());
    }

    @Test
    void updateCollection_duplicateNameOfOtherCollection_throwsConflict() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection(1L, "Old")));
        when(collectionRepository.existsByNameIgnoreCaseAndIdNot("Taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> collectionService.updateCollection(1L, new CollectionRequest("Taken", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(collectionRepository, never()).save(any());
    }

    @Test
    void updateCollection_duplicateName_doesNotModifyManagedEntity() {
        DocumentCollection existing = collection(1L, "Old");
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collectionRepository.existsByNameIgnoreCaseAndIdNot("Taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> collectionService.updateCollection(1L, new CollectionRequest(" Taken ", null)))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(existing.getName()).isEqualTo("Old");
    }

    @Test
    void getCollection_unknownId_throwsNotFound() {
        when(collectionRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getCollection(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Collection with id 9 not found");
    }

    @Test
    void updateCollection_updatesFields() {
        DocumentCollection existing = collection(1L, "Old");
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collectionRepository.existsByNameIgnoreCaseAndIdNot("New", 1L)).thenReturn(false);
        when(collectionRepository.save(existing)).thenReturn(existing);
        when(documentRepository.countByCollectionId(1L)).thenReturn(2L);

        CollectionResponse result = collectionService.updateCollection(1L, new CollectionRequest("New", "Desc"));

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.description()).isEqualTo("Desc");
        assertThat(result.documentCount()).isEqualTo(2L);
    }

    @Test
    void deleteCollection_unassignsDocumentsBeforeDeleting() {
        DocumentCollection existing = collection(1L, "Invoices");
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(existing));

        collectionService.deleteCollection(1L);

        InOrder inOrder = inOrder(documentRepository, collectionRepository);
        inOrder.verify(documentRepository).unassignAllFromCollection(1L);
        inOrder.verify(collectionRepository).delete(existing);
    }

    @Test
    void deleteCollection_unknownId_throwsNotFound() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.deleteCollection(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(documentRepository);
    }

    @Test
    void getDocumentsOfCollection_unknownCollection_throwsNotFound() {
        when(collectionRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getDocumentsOfCollection(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDocumentsOfCollection_returnsDocuments() {
        DocumentCollection invoices = collection(1L, "Invoices");
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(invoices));
        when(documentRepository.findAllByCollectionId(1L, DocumentService.NEWEST_FIRST))
                .thenReturn(List.of(document(10L, invoices), document(11L, invoices)));

        List<DocumentResponse> result = collectionService.getDocumentsOfCollection(1L);

        assertThat(result).extracting(DocumentResponse::id).containsExactly(10L, 11L);
    }

    @Test
    void assignDocument_setsCollection() {
        DocumentCollection invoices = collection(1L, "Invoices");
        Document doc = document(10L, null);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(invoices));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(doc)).thenReturn(doc);

        DocumentResponse result = collectionService.assignDocument(1L, 10L);

        assertThat(doc.getCollection()).isSameAs(invoices);
        assertThat(result.collectionName()).isEqualTo("Invoices");
    }

    @Test
    void assignDocument_unknownDocument_throwsNotFound() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection(1L, "Invoices")));
        when(documentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.assignDocument(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeDocument_clearsCollection() {
        DocumentCollection invoices = collection(1L, "Invoices");
        Document doc = document(10L, invoices);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(invoices));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));
        when(documentRepository.save(doc)).thenReturn(doc);

        DocumentResponse result = collectionService.removeDocument(1L, 10L);

        assertThat(doc.getCollection()).isNull();
        assertThat(result.collectionId()).isNull();
    }

    @Test
    void removeDocument_documentInOtherCollection_throwsNotFound() {
        Document doc = document(10L, collection(2L, "Contracts"));
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection(1L, "Invoices")));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> collectionService.removeDocument(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not part of collection");
        verify(documentRepository, never()).save(any());
    }

    @Test
    void removeDocument_unassignedDocument_throwsNotFound() {
        Document doc = document(10L, null);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection(1L, "Invoices")));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> collectionService.removeDocument(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not part of collection");
        verify(documentRepository, never()).save(any());
    }

    @Test
    void assignDocument_unknownCollection_throwsNotFoundWithoutTouchingDocuments() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.assignDocument(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(documentRepository);
    }
}
