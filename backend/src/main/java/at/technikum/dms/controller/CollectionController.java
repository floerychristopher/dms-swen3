package at.technikum.dms.controller;

import at.technikum.dms.dto.CollectionRequest;
import at.technikum.dms.dto.CollectionResponse;
import at.technikum.dms.dto.DocumentResponse;
import at.technikum.dms.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Tag(name = "Collections", description = "Organise documents in collections (additional use-case)")
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    @Operation(summary = "List all collections (sorted by name) incl. document count")
    public List<CollectionResponse> getAllCollections() {
        return collectionService.getAllCollections();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single collection")
    @ApiResponse(responseCode = "200", description = "Collection found")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    public CollectionResponse getCollection(@PathVariable Long id) {
        return collectionService.getCollection(id);
    }

    @PostMapping
    @Operation(summary = "Create a collection")
    @ApiResponse(responseCode = "201", description = "Collection created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Name already in use")
    public ResponseEntity<CollectionResponse> createCollection(@Valid @RequestBody CollectionRequest request) {
        CollectionResponse created = collectionService.createCollection(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a collection")
    @ApiResponse(responseCode = "200", description = "Collection updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    @ApiResponse(responseCode = "409", description = "Name already in use")
    public CollectionResponse updateCollection(@PathVariable Long id, @Valid @RequestBody CollectionRequest request) {
        return collectionService.updateCollection(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a collection (its documents are kept, but unassigned)")
    @ApiResponse(responseCode = "204", description = "Collection deleted")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long id) {
        collectionService.deleteCollection(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "List all documents of a collection")
    @ApiResponse(responseCode = "200", description = "Documents of the collection")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    public List<DocumentResponse> getDocumentsOfCollection(@PathVariable Long id) {
        return collectionService.getDocumentsOfCollection(id);
    }

    @PutMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Assign a document to this collection (moves it if already in another one)")
    @ApiResponse(responseCode = "200", description = "Document assigned")
    @ApiResponse(responseCode = "404", description = "Collection or document not found")
    public DocumentResponse assignDocument(@PathVariable Long id, @PathVariable Long documentId) {
        return collectionService.assignDocument(id, documentId);
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Remove a document from this collection (the document itself is kept)")
    @ApiResponse(responseCode = "200", description = "Document removed from collection")
    @ApiResponse(responseCode = "404", description = "Collection/document not found or document not in collection")
    public DocumentResponse removeDocument(@PathVariable Long id, @PathVariable Long documentId) {
        return collectionService.removeDocument(id, documentId);
    }
}