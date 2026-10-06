package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableAudit;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentActionStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentActionStateDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentActionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentDraftMutationService {

    private final TimetableEntryRepository entryRepository;
    private final TimetableAuditRepository auditRepository;
    private final TimetableAgentActionRepository actionRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public ResTimetableAgentActionStateDTO apply(TimetableRevision revision, TimetableAgentAction action,
            TimetableAgentProposal proposal, TimetableAgentValidationResult validation,
            TimetableAgentSnapshot snapshot, LocalDate validFrom, LocalDate validTo) {
        List<TimetableEntry> targetRows = entryRepository.findByRevisionId(revision.getId());
        Set<Long> scopedAssignmentIds = snapshot.assignments().stream()
                .map(TimetableAgentSnapshot.TimetableAgentAssignmentOption::assignmentId)
                .collect(java.util.stream.Collectors.toSet());
        List<TimetableEntry> replaceable = targetRows.stream()
                .filter(entry -> scopedAssignmentIds.contains(entry.getAssignmentId()))
                .filter(entry -> overlaps(entry.getValidFrom(), entry.getValidTo(), validFrom, validTo))
                .toList();
        List<TimetableEntry> unmatchedCandidates = new ArrayList<>();
        Set<Long> preservedEntryIds = new HashSet<>();
        for (TimetableAgentProposalEntryDTO candidate : validation.entries()) {
            TimetableEntry unchanged = replaceable.stream().filter(old -> !preservedEntryIds.contains(old.getId()))
                    .filter(old -> sameValue(old, candidate)).findFirst().orElse(null);
            if (unchanged == null) {
                unmatchedCandidates.add(new TimetableEntry(revision.getId(), candidate.assignmentId(),
                        candidate.periodId(), candidate.functionalRoomId(), candidate.validFrom(),
                        candidate.validTo()));
            } else {
                preservedEntryIds.add(unchanged.getId());
            }
        }
        List<TimetableEntry> toDelete = replaceable.stream()
                .filter(old -> !preservedEntryIds.contains(old.getId())).toList();
        entryRepository.deleteAll(toDelete);
        entryRepository.flush();
        List<TimetableEntry> replacements = splitBoundaryRows(toDelete, validFrom, validTo, revision.getId());
        replacements.addAll(unmatchedCandidates);
        entryRepository.saveAllAndFlush(replacements);
        entityManager.lock(revision, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        entityManager.flush();

        Instant committedAt = Instant.now(Clock.systemUTC());
        auditRepository.save(new TimetableAudit(revision.getTimetableId(), revision.getId(),
                "AI_SAVE_DRAFT", AuditContext.currentUserId(), "actionId=" + action.getId()
                        + ", proposalId=" + proposal.getId() + ", savedEntryCount=" + replacements.size()));
        action.setStatus("SAVED_DRAFT");
        action.setCommittedAt(committedAt);
        action.setErrorCode(null);
        proposal.setStatus(com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus.SAVED);
        ResTimetableAgentActionStateDTO receipt = new ResTimetableAgentActionStateDTO(action.getId().toString(),
                proposal.getId().toString(), revision.getId(), TimetableAgentActionStatus.SAVED_DRAFT,
                revision.getVersion(), unmatchedCandidates.size(), committedAt, null, null, null);
        action.setReceiptJson(write(receipt));
        actionRepository.saveAndFlush(action);
        return receipt;
    }

    private List<TimetableEntry> splitBoundaryRows(List<TimetableEntry> rows, LocalDate validFrom,
            LocalDate validTo, Long revisionId) {
        List<TimetableEntry> replacements = new ArrayList<>();
        for (TimetableEntry old : rows) {
            if (old.getValidFrom().isBefore(validFrom)) {
                replacements.add(copy(old, revisionId, old.getValidFrom(), validFrom.minusDays(1)));
            }
            if (old.getValidTo().isAfter(validTo)) {
                replacements.add(copy(old, revisionId, validTo.plusDays(1), old.getValidTo()));
            }
        }
        return replacements;
    }

    private TimetableEntry copy(TimetableEntry source, Long revisionId, LocalDate from, LocalDate to) {
        return new TimetableEntry(revisionId, source.getAssignmentId(), source.getPeriodId(),
                source.getFunctionalRoomId(), from, to);
    }

    private boolean overlaps(LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
        return !aFrom.isAfter(bTo) && !aTo.isBefore(bFrom);
    }

    private boolean sameValue(TimetableEntry old, TimetableAgentProposalEntryDTO candidate) {
        return Objects.equals(old.getAssignmentId(), candidate.assignmentId())
                && Objects.equals(old.getPeriodId(), candidate.periodId())
                && Objects.equals(old.getFunctionalRoomId(), candidate.functionalRoomId())
                && Objects.equals(old.getValidFrom(), candidate.validFrom())
                && Objects.equals(old.getValidTo(), candidate.validTo());
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Action receipt could not be stored.", exception);
        }
    }

}
