package at.technikum.dms.controller;

import at.technikum.dms.dto.DocumentRequest;
import at.technikum.dms.dto.DocumentResponse;
import at.technikum.dms.exception.ResourceNotFoundException;
import at.technikum.dms.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-Layer-Tests: nur Controller + GlobalExceptionHandler werden geladen, der Service ist gemockt.
 */
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    private static DocumentResponse response(Long id, String title) {
        return new DocumentResponse(id, title, "Desc", LocalDateTime.of(2026, 10, 1, 12, 0), null, null, null);
    }

    @Test
    void getAllDocuments_returns200WithList() throws Exception {
        when(documentService.getAllDocuments()).thenReturn(List.of(response(1L, "Test Title")));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Title"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-01T12:00:00"));
    }

    @Test
    void getDocument_unknownId_returns404ProblemDetail() throws Exception {
        when(documentService.getDocument(99L)).thenThrow(new ResourceNotFoundException("Document", 99L));

        mockMvc.perform(get("/api/documents/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Document with id 99 not found"));
    }

    @Test
    void getDocument_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/documents/abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void createDocument_valid_returns201WithLocation() throws Exception {
        when(documentService.createDocument(any())).thenReturn(response(2L, "New Doc"));

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "New Doc", "description": "Desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/documents/2")))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("New Doc"));

        verify(documentService).createDocument(new DocumentRequest("New Doc", "Desc", null));
    }

    @Test
    void createDocument_blankTitle_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").value("Title is required"));
        verifyNoInteractions(documentService);
    }

    @Test
    void createDocument_tooLongTitle_returns400() throws Exception {
        String longTitle = "x".repeat(256);
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + longTitle + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Title must be at most 255 characters"));
    }

    @Test
    void createDocument_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }

    @Test
    void updateDocument_valid_returns200() throws Exception {
        when(documentService.updateDocument(eq(1L), any())).thenReturn(response(1L, "Updated"));

        mockMvc.perform(put("/api/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Updated", "collectionId": 3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));

        verify(documentService).updateDocument(1L, new DocumentRequest("Updated", null, 3L));
    }

    @Test
    void deleteDocument_returns204() throws Exception {
        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isNoContent());
        verify(documentService).deleteDocument(1L);
    }

    @Test
    void deleteDocument_unknownId_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Document", 5L)).when(documentService).deleteDocument(5L);

        mockMvc.perform(delete("/api/documents/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDocument_nonPositiveCollectionId_returns400() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Doc", "collectionId": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.collectionId").value("Collection id must be positive"));
        verifyNoInteractions(documentService);
    }

    @Test
    void updateDocument_unknownCollection_returns404() throws Exception {
        when(documentService.updateDocument(eq(1L), any()))
                .thenThrow(new ResourceNotFoundException("Collection", 99L));

        mockMvc.perform(put("/api/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Doc", "collectionId": 99}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Collection with id 99 not found"));
    }
}
