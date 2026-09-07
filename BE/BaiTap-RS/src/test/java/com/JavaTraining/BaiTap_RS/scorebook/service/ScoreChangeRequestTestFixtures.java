package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequest;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreSnapshotStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import org.springframework.test.util.ReflectionTestUtils;

final class ScoreChangeRequestTestFixtures {

    private ScoreChangeRequestTestFixtures() {
    }

    /* default */ static ScoreChangeRequest request(
            Long id, Long columnId, Long studentId, Long requestedBy,
            ScoreSnapshotStatus beforeStatus, BigDecimal beforeValue,
            ScoreStatus proposedStatus, BigDecimal proposedValue) {
        ScoreChangeRequest request = new ScoreChangeRequest(
                columnId,
                studentId,
                beforeStatus == ScoreSnapshotStatus.UNSCORED ? null : 300L,
                beforeStatus,
                beforeValue,
                proposedStatus,
                proposedValue,
                "Cập nhật điểm theo biên bản",
                requestedBy,
                LocalDateTime.now());
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    /* default */ static StudentScore score(
            Long id, Long columnId, Long studentId, ScoreStatus status, BigDecimal value) {
        StudentScore score = new StudentScore(columnId, studentId, status, value, null, 100L);
        ReflectionTestUtils.setField(score, "id", id);
        return score;
    }
}
