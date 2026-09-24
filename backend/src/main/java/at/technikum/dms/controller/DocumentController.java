package at.technikum.dms.controller;

import at.technikum.dms.entity.Document;
import at.technikum.dms.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<Document> uploadDocument(@RequestBody Document document) {
        // Später: MultipartFile (PDF) entgegennehmen
        Document savedDoc = documentService.createDocument(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDoc);
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }
}