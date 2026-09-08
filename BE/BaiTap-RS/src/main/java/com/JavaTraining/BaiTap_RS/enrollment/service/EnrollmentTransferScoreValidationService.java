package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferTargetScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookGuard;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScoreEntryValidator;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import org.springframework.http.HttpStatus;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentTransferScoreValidationService {

    private final ScorebookGuard scorebookGuard;
    private final ScoreEntryValidator scoreValidator;

    public EnrollmentTransferScoreValidationService(
            ScorebookGuard scorebookGuard, ScoreEntryValidator scoreValidator) {
        this.scorebookGuard = scorebookGuard;
        this.scoreValidator = scoreValidator;
    }

    public List<PreparedTransferScore> validateScores(TransferScoreValidationRequest request) {
        Set<Long> seen = new HashSet<>();
        Map<Long, AssessmentColumn> columns = request.scoreData().targetColumns().stream()
                .collect(Collectors.toMap(AssessmentColumn::getId, Function.identity()));
        Map<Long, StudentScore> existing = request.scoreData().targetScores();
        return request.requests().stream().map(item -> {
            if (!seen.add(item.assessmentColumnId())) {
                throw badRequest("Trùng cột điểm đích: " + item.assessmentColumnId());
            }
            AssessmentColumn column = columns.get(item.assessmentColumnId());
            if (column == null || column.getStatus() != AssessmentColumnStatus.ACTIVE) {
                throw conflict("Cột điểm đích không tồn tại hoặc không còn hoạt động");
            }
            Scorebook scorebook = request.scoreData().targetScorebookByColumnId().get(column.getId());
            if (scorebook == null || !isWritable(scorebook.getStatus())) {
                throw conflict("Sổ điểm đích chưa sẵn sàng để nhập điểm");
            }
            scorebookGuard.assertCanManage(scorebook);
            scoreValidator.validateScoreValue(item.scoreStatus(), item.scoreValue());
            StudentScore current = existing.get(column.getId());
            if (current == null) {
                scoreValidator.validateCreateVersion(item.expectedVersion());
            } else {
                scoreValidator.validateVersion(current, item.expectedVersion());
                scoreValidator.validateUpdateEligibility(current, request.semester());
            }
            return new PreparedTransferScore(item, column, current);
        }).toList();
    }

    private boolean isWritable(ScorebookStatus status) {
        return status == ScorebookStatus.OPEN || status == ScorebookStatus.PUBLISHED;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }

    private AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, message);
    }
}

record TransferScoreValidationRequest(
        List<ReqTransferTargetScoreDTO> requests,
        TransferScoreData scoreData,
        Student student,
        Semester semester) {
}

record PreparedTransferScore(
        ReqTransferTargetScoreDTO request,
        AssessmentColumn column,
        StudentScore existing) {
}
