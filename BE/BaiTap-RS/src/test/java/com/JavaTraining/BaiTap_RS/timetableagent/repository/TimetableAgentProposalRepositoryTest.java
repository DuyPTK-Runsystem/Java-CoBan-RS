package com.JavaTraining.BaiTap_RS.timetableagent.repository;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentActionStateDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentDiffDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalIdentity;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalPayload;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentDraftMutationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.Query;
import org.springframework.test.util.ReflectionTestUtils;

class TimetableAgentProposalRepositoryTest {

    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url",
                        "jdbc:h2:mem:timetable-agent-proposal-repository;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.show_sql", "false")
                .build();
        sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(TimetableAgentProposal.class)
                .addAnnotatedClass(TimetableRevision.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    @AfterEach
    void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void embeddedActorQueryFindsOnlyTheOwningActorProposal() throws Exception {
        try (EntityManager entityManager = sessionFactory.createEntityManager()) {
            entityManager.getTransaction().begin();
            TimetableAgentProposal saved = proposal(41L);
            entityManager.persist(saved);
            entityManager.flush();

            Query repositoryQuery = repositoryQuery();
            Optional<Long> ownerResult = findId(entityManager, repositoryQuery, saved.getId(), 41L);
            Optional<Long> otherActorResult = findId(entityManager, repositoryQuery, saved.getId(), 42L);
            boolean queryIsActorScoped = ownerResult.equals(Optional.of(saved.getId()))
                    && otherActorResult.isEmpty();

            Assertions.assertTrue(queryIsActorScoped,
                    "The repository's embedded identity query must return a proposal only for its owner.");
            entityManager.getTransaction().rollback();
        }
    }

    @Test
    void mutationServiceReceiptVersionMatchesCommittedRevisionVersion() {
        TimetableRevision revision = new TimetableRevision(81L, 5L, 1,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), 14L);
        Long revisionId;
        try (EntityManager entityManager = sessionFactory.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.persist(revision);
            entityManager.flush();
            revisionId = revision.getId();
            entityManager.getTransaction().commit();
        }

        Long receiptVersion;
        try (EntityManager entityManager = sessionFactory.createEntityManager()) {
            entityManager.getTransaction().begin();
            TimetableRevision managed = entityManager.find(TimetableRevision.class, revisionId);
            TimetableEntryRepository entryRepository = Mockito.mock(TimetableEntryRepository.class);
            Mockito.when(entryRepository.findByRevisionId(revisionId)).thenReturn(List.of());
            TimetableAgentAction action = new TimetableAgentAction(31L, 41L, "idempotency-key", "request-hash", null);
            ReflectionTestUtils.setField(action, "id", 55L);
            TimetableAgentProposal proposal = proposal(41L);
            ReflectionTestUtils.setField(proposal, "id", 31L);
            TimetableAgentDraftMutationService mutationService = new TimetableAgentDraftMutationService(
                    entryRepository, Mockito.mock(TimetableAuditRepository.class),
                    Mockito.mock(TimetableAgentActionRepository.class),
                    entityManager, new ObjectMapper().findAndRegisterModules());
            ResTimetableAgentActionStateDTO receipt = mutationService.apply(managed, action, proposal,
                    readyResult(), emptySnapshot(revisionId), LocalDate.of(2026, 10, 5),
                    LocalDate.of(2026, 12, 31));
            receiptVersion = receipt.newVersion();
            entityManager.getTransaction().commit();
        }

        try (EntityManager entityManager = sessionFactory.createEntityManager()) {
            Long committedVersion = entityManager.find(TimetableRevision.class, revisionId).getVersion();

            Assertions.assertEquals(committedVersion, receiptVersion,
                    "The version captured for the receipt must match the committed revision version.");
        }
    }

    private TimetableAgentValidationResult readyResult() {
        return new TimetableAgentValidationResult(
                com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus.READY_FOR_REVIEW,
                List.of(), List.of(), new ResTimetableAgentDiffDTO(List.of(), List.of(), List.of()), "ready");
    }

    private TimetableAgentSnapshot emptySnapshot(Long revisionId) {
        LocalDate validFrom = LocalDate.of(2026, 10, 5);
        LocalDate validTo = LocalDate.of(2026, 12, 31);
        return new TimetableAgentSnapshot("snapshot-1", "fingerprint", "{}", "{}", "{}", 41L,
                revisionId, 5L, 0L, validFrom, validTo, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    private Optional<Long> findId(EntityManager entityManager, Query repositoryQuery, Long id, Long actorId) {
        TypedQuery<TimetableAgentProposal> query = entityManager.createQuery(repositoryQuery.value(),
                TimetableAgentProposal.class);
        query.setParameter("id", id);
        query.setParameter("actorId", actorId);
        return query.getResultList().stream().map(TimetableAgentProposal::getId).findFirst();
    }

    private Query repositoryQuery() throws NoSuchMethodException {
        Method method = TimetableAgentProposalRepository.class.getMethod("findByIdAndActorId", Long.class, Long.class);
        return method.getAnnotation(Query.class);
    }

    private TimetableAgentProposal proposal(Long actorId) {
        return new TimetableAgentProposal(
                new TimetableAgentProposalIdentity(actorId, 77L, 5L, 9L, 2L, "snapshot-1",
                        "snapshot-fingerprint", "proposal-hash"),
                new TimetableAgentProposalPayload("{}", "{}", "{}", "{}", "[]", "{}"),
                TimetableAgentProposalStatus.READY_FOR_REVIEW, Instant.now().plusSeconds(60));
    }
}

