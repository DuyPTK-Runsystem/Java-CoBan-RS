package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequest;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreSnapshotStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScoreChangeRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.TooManyMethods")
public class ScoreChangeRequestValidator {

    private static final String PENDING_CONFLICT_MESSAGE = "Ô điểm đã có yêu cầu sửa đang chờ duyệt";
    private static final String SNAPSHOT_CONFLICT_MESSAGE = "Dữ liệu điểm hiện tại đã thay đổi so với snapshot";

    private final ScoreChangeRequestRepository requestRepository;
    private final ScoreEntryValidator scoreValidator;

    public ScoreChangeRequestValidator(
            ScoreChangeRequestRepository requestRepository,
            ScoreEntryValidator scoreValidator) {
        this.requestRepository = requestRepository;
        this.scoreValidator = scoreValidator;
    }

    public void validateProposedScore(ScoreStatus status, BigDecimal value) {
        scoreValidator.validateScoreValue(status, value);
    }

    public void validatePendingConflict(Long columnId, Long studentId) {
        if (requestRepository.existsByAssessmentColumnIdAndStudentIdAndStatus(
                columnId, studentId, ScoreChangeRequestStatus.PENDING)) {
            throw conflict(PENDING_CONFLICT_MESSAGE);
        }
    }

    public void validateDifferentFromCurrent(
            Optional<StudentScore> current, ScoreStatus proposedStatus, BigDecimal proposedValue) {
        if (current.isPresent()
                && current.get().getScoreStatus() == proposedStatus
                && equalValue(current.get().getScoreValue(), proposedValue)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Điểm đề xuất phải khác điểm hiện tại");
        }
    }

    public void validatePending(ScoreChangeRequest request) {
        if (request.getStatus() != ScoreChangeRequestStatus.PENDING) {
            throw conflict("Yêu cầu không còn ở trạng thái PENDING");
        }
    }

    public void validateNotSelfReview(ScoreChangeRequest request, Long reviewerId) {
        if (reviewerId.equals(request.getRequestedBy())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Người yêu cầu không được tự phê duyệt hoặc từ chối");
        }
    }

    public void validateSnapshotMatch(ScoreChangeRequest request, Optional<StudentScore> current) {
        if (request.getStudentScoreId() == null) {
            validateMissingScoreSnapshot(current);
            return;
        }
        if (current.isEmpty() || !matchesExistingScore(request, current.get())) {
            throw conflict(SNAPSHOT_CONFLICT_MESSAGE);
        }
    }

    private void validateMissingScoreSnapshot(Optional<StudentScore> current) {
        if (current.isPresent()) {
            throw conflict(SNAPSHOT_CONFLICT_MESSAGE);
        }
    }

    private boolean matchesExistingScore(ScoreChangeRequest request, StudentScore score) {
        return request.getStudentScoreId().equals(score.getId())
                && request.getBeforeStatus().equals(snapshotStatus(score.getScoreStatus()))
                && equalValue(request.getBeforeValue(), score.getScoreValue());
    }

    private ScoreSnapshotStatus snapshotStatus(ScoreStatus status) {
        return ScoreSnapshotStatus.valueOf(status.name());
    }

    private boolean equalValue(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
