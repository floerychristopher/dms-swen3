package at.technikum.dms.service;

import at.technikum.dms.entity.Document;
import at.technikum.dms.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void createDocument_ShouldSetCreatedAtAndSave() {
        // Arrange (Vorbereitung)
        Document docToSave = new Document("Test PDF", "Test Description");
        docToSave.setCreatedAt(null); // Simulieren, dass das Datum noch fehlt

        Document savedDoc = new Document("Test PDF", "Test Description");
        savedDoc.setId(1L);
        savedDoc.setCreatedAt(LocalDateTime.now());

        // Was tun wenn save() auferufen wird
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);

        // Act (Ausführung)
        Document result = documentService.createDocument(docToSave);

        // Assert (Überprüfung)
        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
        verify(documentRepository, times(1)).save(docToSave); // Prüft, ob die DB 1x aufgerufen wurde
    }

    @Test
    void getAllDocuments_ShouldReturnList() {
        // Arrange
        when(documentRepository.findAll()).thenReturn(List.of(new Document("Doc 1", "Desc 1")));

        // Act
        List<Document> result = documentService.getAllDocuments();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Doc 1", result.get(0).getTitle());
    }
}