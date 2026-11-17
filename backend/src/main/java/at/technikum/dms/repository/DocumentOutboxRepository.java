package at.technikum.dms.repository;

import at.technikum.dms.messaging.DocumentCreatedMessage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DocumentOutboxRepository extends JpaRepository<DocumentCreatedMessage, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentCreatedMessage> findFirstByOrderByDocumentId();
}
