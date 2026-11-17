package at.technikum.dms.controller;

import at.technikum.dms.dto.DocumentRequest;
import at.technikum.dms.dto.DocumentResponse;
import at.technikum.dms.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Manage documents (metadata)")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "List all documents (newest first)")
    public List<DocumentResponse> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single document")
    @ApiResponse(responseCode = "200", description = "Document found")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public DocumentResponse getDocument(@PathVariable Long id) {
        return documentService.getDocument(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a document")
    @ApiResponse(responseCode = "201", description = "Document created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Referenced collection not found")
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentRequest request) {
        return createdResponse(documentService.createDocument(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF and queue it for OCR")
    public ResponseEntity<DocumentResponse> uploadDocument(@Valid @ModelAttribute DocumentRequest request,
                                                            @RequestParam("file") MultipartFile file) {
        return createdResponse(documentService.createDocument(request, file));
    }

    private ResponseEntity<DocumentResponse> createdResponse(DocumentResponse created) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a document (collectionId = null removes it from its collection)")
    @ApiResponse(responseCode = "200", description = "Document updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Document or collection not found")
    public DocumentResponse updateDocument(@PathVariable Long id, @Valid @RequestBody DocumentRequest request) {
        return documentService.updateDocument(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    @ApiResponse(responseCode = "204", description = "Document deleted")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
