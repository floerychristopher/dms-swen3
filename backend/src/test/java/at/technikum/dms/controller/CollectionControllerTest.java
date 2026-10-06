package at.technikum.dms.controller;

import at.technikum.dms.dto.CollectionRequest;
import at.technikum.dms.dto.CollectionResponse;
import at.technikum.dms.dto.DocumentResponse;
import at.technikum.dms.exception.DuplicateResourceException;
import at.technikum.dms.exception.ResourceNotFoundException;
import at.technikum.dms.service.CollectionService;
import org.springframework.dao.DataIntegrityViolationException;
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

@WebMvcTest(CollectionController.class)
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionService collectionService;

    private static CollectionResponse collection(Long id, String name, long count) {
        return new CollectionResponse(id, name, null, LocalDateTime.of(2026, 10, 1, 12, 0), count);
    }

    private static DocumentResponse document(Long id, Long collectionId, String collectionName) {
        return new DocumentResponse(id, "Doc " + id, null, LocalDateTime.of(2026, 10, 1, 12, 0), null,
                collectionId, collectionName);
    }

    @Test
    void getAllCollections_returns200() throws Exception {
        when(collectionService.getAllCollections()).thenReturn(List.of(collection(1L, "Invoices", 3)));

        mockMvc.perform(get("/api/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Invoices"))
                .andExpect(jsonPath("$[0].documentCount").value(3));
    }

    @Test
    void getCollection_unknownId_returns404() throws Exception {
        when(collectionService.getCollection(9L)).thenThrow(new ResourceNotFoundException("Collection", 9L));

        mockMvc.perform(get("/api/collections/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Collection with id 9 not found"));
    }

    @Test
    void createCollection_valid_returns201() throws Exception {
        when(collectionService.createCollection(any())).thenReturn(collection(4L, "Invoices", 0));

        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Invoices", "description": "All invoices"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/collections/4")))
                .andExpect(jsonPath("$.id").value(4));

        verify(collectionService).createCollection(new CollectionRequest("Invoices", "All invoices"));
    }

    @Test
    void createCollection_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name is required"));
        verifyNoInteractions(collectionService);
    }

    @Test
    void createCollection_duplicateName_returns409WithFieldError() throws Exception {
        when(collectionService.createCollection(any()))
                .thenThrow(new DuplicateResourceException("name", "A collection named 'Invoices' already exists"));

        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Invoices"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.name").value("A collection named 'Invoices' already exists"));
    }

    @Test
    void updateCollection_valid_returns200() throws Exception {
        when(collectionService.updateCollection(eq(1L), any())).thenReturn(collection(1L, "Renamed", 2));

        mockMvc.perform(put("/api/collections/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void deleteCollection_returns204() throws Exception {
        mockMvc.perform(delete("/api/collections/1"))
                .andExpect(status().isNoContent());
        verify(collectionService).deleteCollection(1L);
    }

    @Test
    void getDocumentsOfCollection_returns200() throws Exception {
        when(collectionService.getDocumentsOfCollection(1L))
                .thenReturn(List.of(document(10L, 1L, "Invoices")));

        mockMvc.perform(get("/api/collections/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].collectionName").value("Invoices"));
    }

    @Test
    void assignDocument_returns200() throws Exception {
        when(collectionService.assignDocument(1L, 10L)).thenReturn(document(10L, 1L, "Invoices"));

        mockMvc.perform(put("/api/collections/1/documents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionId").value(1));
    }

    @Test
    void removeDocument_notInCollection_returns404() throws Exception {
        when(collectionService.removeDocument(1L, 10L))
                .thenThrow(new ResourceNotFoundException("Document with id 10 is not part of collection 1"));

        mockMvc.perform(delete("/api/collections/1/documents/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCollection_tooLongName_returns400() throws Exception {
        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Name must be at most 100 characters"));
        verifyNoInteractions(collectionService);
    }

    @Test
    void updateCollection_unknownId_returns404() throws Exception {
        when(collectionService.updateCollection(eq(9L), any())).thenThrow(new ResourceNotFoundException("Collection", 9L));

        mockMvc.perform(put("/api/collections/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Renamed"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void dataIntegrityViolation_returns409WithoutLeakingDbDetails() throws Exception {
        when(collectionService.createCollection(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uk_123"));

        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Invoices"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The request conflicts with existing data"));
    }

    @Test
    void unexpectedException_returns500ProblemDetail() throws Exception {
        when(collectionService.getAllCollections()).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/collections"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
