package com.JavaTraining.BaiTap_RS.library.catalog.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryCatalogAuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @Test
    void storesBeforeAndAfterAsJsonWithCatalogActionAndEntityIdentity() throws Exception {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", "AVAILABLE");
        before.put("shelfLocation", "A1");
        Map<String, Object> after = Map.of("status", "DAMAGED", "shelfLocation", "B2");

        service().record("BOOK_COPY_UPDATED", "book_copy", 81L, before, after);

        ObjectMapper mapper = new ObjectMapper();
        Mockito.verify(repository, Mockito.description(
                        "Audit row should retain action, identity, and both serialized snapshots"))
                .save(Mockito.argThat(saved -> {
                    try {
                        return "BOOK_COPY_UPDATED".equals(saved.getAction())
                                && "book_copy".equals(saved.getEntityType())
                                && "81".equals(saved.getEntityId())
                                && mapper.readTree(saved.getBeforeData()).equals(mapper.valueToTree(before))
                                && mapper.readTree(saved.getAfterData()).equals(mapper.valueToTree(after));
                    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
                        return false;
                    }
                }));
    }

    @Test
    void nullSnapshotsRemainNullAndPendingEntityUsesPendingIdentity() {
        service().record("BOOK_CREATED", "book", null, null, Map.of("title", "New"));

        Mockito.verify(repository, Mockito.description(
                        "Pending identity, absent before snapshot, and new title snapshot should be preserved"))
                .save(Mockito.argThat(saved -> "pending".equals(saved.getEntityId())
                        && saved.getBeforeData() == null
                        && "{\"title\":\"New\"}".equals(saved.getAfterData())));
    }

    @Test
    void repositoryFailurePropagatesToKeepAuditInsideMutationTransaction() {
        Mockito.doThrow(new IllegalStateException("storage down")).when(repository)
                .save(org.mockito.ArgumentMatchers.any(AuditLog.class));

        Assertions.assertThrows(IllegalStateException.class,
                () -> service().record("BOOK_ARCHIVED", "book", 9L, Map.of("archived", false), null),
                "Audit persistence failure should propagate to the caller");
    }

    private LibraryCatalogAuditService service() {
        return new LibraryCatalogAuditService(repository, new ObjectMapper());
    }
}
