package at.technikum.dms.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "document_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentCreatedMessage {
    @Id
    private Long documentId;
    private String title;
    private String storageKey;

    public DocumentCreatedMessage(Long documentId, String title, String storageKey) {
        this.documentId = documentId;
        this.title = title;
        this.storageKey = storageKey;
    }
}
