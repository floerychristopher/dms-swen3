package at.technikum.dms.service;

import at.technikum.dms.dto.DocumentRequest;
import at.technikum.dms.dto.DocumentResponse;
import at.technikum.dms.entity.Document;
import at.technikum.dms.entity.DocumentCollection;
import at.technikum.dms.exception.ResourceNotFoundException;
import at.technikum.dms.mapper.DocumentMapper;
import at.technikum.dms.repository.CollectionRepository;
import at.technikum.dms.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für den DocumentService. Die "Produktions"-Datenbank wird durch Mockito-Mocks
 * der Repositories ersetzt; der Mapper ist echt (Spy), da er reine Logik ohne Abhängigkeiten ist.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private CollectionRepository collectionRepository;

    @Spy
    private DocumentMapper documentMapper = new DocumentMapper();

    @InjectMocks
    private DocumentService documentService;

    private static Document document(Long id, String title) {
        Document doc = new Document(title, "Desc");
        doc.setId(id);
        doc.setCreatedAt(LocalDateTime.now());
        return doc;
    }

    private static DocumentCollection collection(Long id, String name) {
        return DocumentCollection.builder().id(id).name(name).build();
    }

    @Test
    void getAllDocuments_returnsMappedList() {
        Document doc = document(1L, "Doc 1");
        doc.setCollection(collection(5L, "Invoices"));
        when(documentRepository.findAllBy(DocumentService.NEWEST_FIRST)).thenReturn(List.of(doc));

        List<DocumentResponse> result = documentService.getAllDocuments();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("Doc 1");
        assertThat(result.getFirst().collectionId()).isEqualTo(5L);
        assertThat(result.getFirst().collectionName()).isEqualTo("Invoices");
    }

    @Test
    void getDocument_unknownId_throwsNotFound() {
        when(documentRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void createDocument_trimsInputAndSaves() {
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        DocumentResponse result = documentService.createDocument(new DocumentRequest("  Test PDF  ", "   ", null));

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Test PDF");
        assertThat(captor.getValue().getDescription()).isNull();
        assertThat(captor.getValue().getCollection()).isNull();
        assertThat(result.id()).isEqualTo(1L);
        verifyNoInteractions(collectionRepository);
    }

    @Test
    void createDocument_withCollection_assignsCollection() {
        DocumentCollection invoices = collection(3L, "Invoices");
        when(collectionRepository.findById(3L)).thenReturn(Optional.of(invoices));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse result = documentService.createDocument(new DocumentRequest("Bill", null, 3L));

        assertThat(result.collectionId()).isEqualTo(3L);
        assertThat(result.collectionName()).isEqualTo("Invoices");
    }

    @Test
    void createDocument_unknownCollection_throwsAndDoesNotSave() {
        when(collectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.createDocument(new DocumentRequest("Bill", null, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void updateDocument_updatesFieldsAndRemovesCollectionWhenNull() {
        Document existing = document(1L, "Old");
        existing.setCollection(collection(3L, "Invoices"));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(documentRepository.save(existing)).thenReturn(existing);

        DocumentResponse result = documentService.updateDocument(1L, new DocumentRequest("New", "New desc", null));

        assertThat(result.title()).isEqualTo("New");
        assertThat(result.description()).isEqualTo("New desc");
        assertThat(result.collectionId()).isNull();
    }

    @Test
    void updateDocument_movesDocumentToOtherCollection() {
        Document existing = document(1L, "Doc");
        existing.setCollection(collection(3L, "Invoices"));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collectionRepository.findById(4L)).thenReturn(Optional.of(collection(4L, "Contracts")));
        when(documentRepository.save(existing)).thenReturn(existing);

        DocumentResponse result = documentService.updateDocument(1L, new DocumentRequest("Doc", null, 4L));

        assertThat(result.collectionId()).isEqualTo(4L);
        assertThat(result.collectionName()).isEqualTo("Contracts");
    }

    @Test
    void updateDocument_unknownId_throwsNotFoundAndDoesNotSave() {
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.updateDocument(1L, new DocumentRequest("New", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void updateDocument_unknownCollection_throwsNotFoundAndDoesNotSave() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document(1L, "Doc")));
        when(collectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.updateDocument(1L, new DocumentRequest("Doc", null, 99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Collection with id 99 not found");
        verify(documentRepository, never()).save(any());
    }

    @Test
    void deleteDocument_deletesExisting() {
        Document existing = document(1L, "Doc");
        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));

        documentService.deleteDocument(1L);

        verify(documentRepository).delete(existing);
    }

    @Test
    void deleteDocument_unknownId_throwsNotFound() {
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteDocument(1L)).isInstanceOf(ResourceNotFoundException.class);
        verify(documentRepository, never()).delete(any());
    }
}