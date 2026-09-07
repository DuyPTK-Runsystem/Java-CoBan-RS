package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqBulkScoreItemDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Component;

/**
 * Helper thực hiện persistence và audit cho student score mutations.
 */
@Component
public class ScoreEntryWriter {

    private static final String ENTITY_TYPE = "StudentScore";

    private final StudentScoreRepository scoreRepository;
    private final ScorebookAuditService auditService;
    private final ScoreAuditDataMapper auditMapper;
    private final ScoreResponseMapper responseMapper;
    private final ScoreEntryValidator validator;

    public ScoreEntryWriter(
            StudentScoreRepository scoreRepository,
            ScorebookAuditService auditService,
            ScoreAuditDataMapper auditMapper,
            ScoreResponseMapper responseMapper,
            ScoreEntryValidator validator) {
        this.scoreRepository = scoreRepository;
        this.auditService = auditService;
        this.auditMapper = auditMapper;
        this.responseMapper = responseMapper;
        this.validator = validator;
    }

    public Optional<StudentScore> findExisting(Long columnId, Long studentId) {
        return scoreRepository.findByAssessmentColumnIdAndStudentId(columnId, studentId);
    }

    public ResStudentScoreDTO createNew(
            Long columnId,
            Student student,
            ScoreStatus status,
            BigDecimal value,
            String note,
            Long actorId) {
        Long studentId = student.getId();
        StudentScore score = new StudentScore(columnId, studentId, status, value, note, actorId);
        score = scoreRepository.save(score);

        auditService.writeAudit(
                "STUDENT_SCORE_CREATED", ENTITY_TYPE, score.getId(),
                null, auditMapper.toSnapshot(score));

        return responseMapper.toResponse(score, student);
    }

    public ResStudentScoreDTO updateExisting(
            StudentScore score, Student student, ReqUpsertStudentScoreDTO request, Semester semester, Long actorId) {
        if (isSingleUnchanged(score, request)) {
            return responseMapper.toResponse(score, student);
        }

        validator.validateVersion(score, request.expectedVersion());
        validator.validateUpdateEligibility(score, semester);

        Map<String, Object> before = auditMapper.toSnapshot(score);
        score.updateScore(request.scoreStatus(), request.scoreValue(), request.note(), actorId);

        auditService.writeAudit(
                "STUDENT_SCORE_UPDATED", ENTITY_TYPE, score.getId(),
                before, auditMapper.toSnapshot(score));

        return responseMapper.toResponse(score, student);
    }

    public ResStudentScoreDTO updateExisting(
            StudentScore score, Student student, ReqBulkScoreItemDTO item, Semester semester, Long actorId) {
        if (isItemUnchanged(score, item)) {
            return responseMapper.toResponse(score, student);
        }

        validator.validateVersion(score, item.expectedVersion());
        validator.validateUpdateEligibility(score, semester);

        Map<String, Object> before = auditMapper.toSnapshot(score);
        score.updateScore(item.scoreStatus(), item.scoreValue(), item.note(), actorId);

        auditService.writeAudit(
                "STUDENT_SCORE_UPDATED", ENTITY_TYPE, score.getId(),
                before, auditMapper.toSnapshot(score));

        return responseMapper.toResponse(score, student);
    }

    public ResStudentScoreDTO toResponse(StudentScore score) {
        return responseMapper.toResponse(score);
    }

    private boolean isSingleUnchanged(StudentScore score, ReqUpsertStudentScoreDTO request) {
        return score.getScoreStatus() == request.scoreStatus()
                && equalsBigDecimal(score.getScoreValue(), request.scoreValue())
                && Objects.equals(score.getNote(), request.note());
    }

    private boolean isItemUnchanged(StudentScore score, ReqBulkScoreItemDTO item) {
        return score.getScoreStatus() == item.scoreStatus()
                && equalsBigDecimal(score.getScoreValue(), item.scoreValue())
                && Objects.equals(score.getNote(), item.note());
    }

    private boolean equalsBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }
}
