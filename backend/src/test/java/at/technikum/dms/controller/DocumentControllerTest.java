package at.technikum.dms.controller;

import at.technikum.dms.entity.Document;
import at.technikum.dms.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController documentController;

    @BeforeEach
    void setUp() {
        // Baut den Controller isoliert für den Test auf, ohne Spring Boot komplett zu starten
        mockMvc = MockMvcBuilders.standaloneSetup(documentController).build();
    }

    @Test
    void getAllDocuments_ShouldReturn200() throws Exception {
        Document doc = new Document("Test Title", "Test Desc");
        doc.setId(1L);
        when(documentService.getAllDocuments()).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Title"))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void uploadDocument_ShouldReturn201() throws Exception {
        Document mockDoc = new Document("New Doc", "Desc");
        mockDoc.setId(2L);

        when(documentService.createDocument(any())).thenReturn(mockDoc);

        String requestBody = "{\"title\": \"New Doc\", \"description\": \"Desc\"}";

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("New Doc"));
    }
}