package at.technikum.dms.repository;

import at.technikum.dms.entity.Document;
import at.technikum.dms.entity.DocumentCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-Tests gegen eine eingebettete H2-Datenbank (kein PostgreSQL nötig).
 */
@DataJpaTest
class DocumentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    private DocumentCollection invoices;
    private DocumentCollection contracts;

    @BeforeEach
    void setUp() {
        invoices = em.persist(DocumentCollection.builder().name("Invoices").build());
        contracts = em.persist(DocumentCollection.builder().name("Contracts").build());

        Document a = new Document("Invoice A", null);
        a.setCollection(invoices);
        Document b = new Document("Invoice B", null);
        b.setCollection(invoices);
        Document c = new Document("Unassigned", null);
        em.persist(a);
        em.persist(b);
        em.persist(c);
        em.flush();
        em.clear();
    }

    @Test
    void prePersist_setsCreatedAt() {
        assertThat(documentRepository.findAll()).allSatisfy(d -> assertThat(d.getCreatedAt()).isNotNull());
        assertThat(collectionRepository.findAll()).allSatisfy(c -> assertThat(c.getCreatedAt()).isNotNull());
    }

    @Test
    void findAllByCollectionId_returnsOnlyDocumentsOfThatCollection() {
        List<Document> result = documentRepository.findAllByCollectionId(invoices.getId(), Sort.by("title"));

        assertThat(result).extracting(Document::getTitle).containsExactly("Invoice A", "Invoice B");
        assertThat(result).allSatisfy(d -> assertThat(d.getCollection().getName()).isEqualTo("Invoices"));
    }

    @Test
    void countByCollectionId_countsDocuments() {
        assertThat(documentRepository.countByCollectionId(invoices.getId())).isEqualTo(2);
        assertThat(documentRepository.countByCollectionId(contracts.getId())).isZero();
    }

    @Test
    void unassignAllFromCollection_keepsDocumentsButRemovesAssignment() {
        int updated = documentRepository.unassignAllFromCollection(invoices.getId());
        em.clear();

        assertThat(updated).isEqualTo(2);
        assertThat(documentRepository.count()).isEqualTo(3);
        assertThat(documentRepository.countByCollectionId(invoices.getId())).isZero();
    }

    @Test
    void existsByNameIgnoreCase_detectsDuplicatesCaseInsensitive() {
        assertThat(collectionRepository.existsByNameIgnoreCase("invoices")).isTrue();
        assertThat(collectionRepository.existsByNameIgnoreCase("Receipts")).isFalse();
        assertThat(collectionRepository.existsByNameIgnoreCaseAndIdNot("INVOICES", invoices.getId())).isFalse();
        assertThat(collectionRepository.existsByNameIgnoreCaseAndIdNot("Invoices", contracts.getId())).isTrue();
    }

    @Test
    void findAllBy_sortsNewestFirst() {
        documentRepository.deleteAll();
        Document older = new Document("Older", null);
        older.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        Document newer = new Document("Newer", null);
        newer.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        em.persist(older);
        em.persist(newer);
        em.flush();
        em.clear();

        List<Document> result = documentRepository.findAllBy(
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id").descending()));

        assertThat(result).extracting(Document::getTitle).containsExactly("Newer", "Older");
    }
}
