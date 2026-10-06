package at.technikum.dms.repository;

import at.technikum.dms.entity.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    /** Alle Dokumente inkl. Collection (verhindert N+1-Queries bei LAZY-Relation). */
    @EntityGraph(attributePaths = "collection")
    List<Document> findAllBy(Sort sort);

    @EntityGraph(attributePaths = "collection")
    List<Document> findAllByCollectionId(Long collectionId, Sort sort);

    long countByCollectionId(Long collectionId);

    /** Löst beim Löschen einer Collection die Zuordnung aller enthaltenen Dokumente. */
    @Modifying
    @Query("update Document d set d.collection = null where d.collection.id = :collectionId")
    int unassignAllFromCollection(@Param("collectionId") Long collectionId);
}