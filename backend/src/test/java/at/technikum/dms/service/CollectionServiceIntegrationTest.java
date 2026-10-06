package at.technikum.dms.service;

import at.technikum.dms.dto.CollectionRequest;
import at.technikum.dms.dto.CollectionResponse;
import at.technikum.dms.entity.Document;
import at.technikum.dms.entity.DocumentCollection;
import at.technikum.dms.exception.DuplicateResourceException;
import at.technikum.dms.mapper.CollectionMapper;
import at.technikum.dms.mapper.DocumentMapper;
import at.technikum.dms.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service + echte JPA-Schicht gegen H2: prüft Verhalten, das mit gemockten Repositories
 * nicht sichtbar ist (Fremdschlüssel, Unique-Constraint, Hibernate-Flush-Verhalten).
 */
@DataJpaTest
@Import({CollectionService.class, CollectionMapper.class, DocumentMapper.class})
class CollectionServiceIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private DocumentRepository documentRepository;

    private DocumentCollection invoices;
    private DocumentCollection contracts;
    private Document invoice;

    @BeforeEach
    void setUp() {
        invoices = em.persist(DocumentCollection.builder().name("Invoices").build());
        contracts = em.persist(DocumentCollection.builder().name("Contracts").build());
        invoice = new Document("Invoice A", null);
        invoice.setCollection(invoices);
        em.persist(invoice);
        em.flush();
        em.clear();
    }

    @Test
    void deleteCollection_keepsDocumentsButUnassignsThem() {
        collectionService.deleteCollection(invoices.getId());
        em.flush();
        em.clear();

        assertThat(em.find(DocumentCollection.class, invoices.getId())).isNull();
        Document reloaded = documentRepository.findById(invoice.getId()).orElseThrow();
        assertThat(reloaded.getCollection()).isNull();
    }

    @Test
    void updateCollection_renameToExactNameOfOtherCollection_throwsDuplicate() {
        assertThatThrownBy(() -> collectionService.updateCollection(contracts.getId(),
                new CollectionRequest("Invoices", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("field").isEqualTo("name");
    }

    @Test
    void updateCollection_changingOnlyCaseOfOwnName_isAllowed() {
        CollectionResponse result = collectionService.updateCollection(invoices.getId(),
                new CollectionRequest("INVOICES", null));

        assertThat(result.name()).isEqualTo("INVOICES");
        assertThat(result.documentCount()).isEqualTo(1);
    }
}
