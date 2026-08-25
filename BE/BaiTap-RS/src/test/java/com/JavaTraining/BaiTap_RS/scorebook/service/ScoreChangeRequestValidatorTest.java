package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreSnapshotStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScoreChangeRequestRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ScoreChangeRequestValidatorTest {

    private static final Long COLUMN_ID = 10L;
    private static final Long STUDENT_ID = 20L;

    @Mock
    private ScoreChangeRequestRepository requestRepository;

    private ScoreChangeRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ScoreChangeRequestValidator(requestRepository, new ScoreEntryValidator());
    }

    @Test
    void validScoredValuePasses() {
        validator.validateProposedScore(ScoreStatus.SCORED, new BigDecimal("8.5"));
    }

    @Test
    void invalidStatusValueCombinationFails() {
        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> validator.validateProposedScore(ScoreStatus.ABSENT, BigDecimal.ONE));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(), "invalid score should be rejected");
    }

    @Test
    void duplicatePendingCellFails() {
        Mockito.when(requestRepository.existsByAssessmentColumnIdAndStudentIdAndStatus(
                COLUMN_ID, STUDENT_ID, ScoreChangeRequestStatus.PENDING)).thenReturn(true);

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> validator.validatePendingConflict(COLUMN_ID, STUDENT_ID));

        Assertions.assertEquals(
                HttpStatus.CONFLICT, exception.getStatus(), "duplicate pending request should conflict");
    }

    @Test
    void equalCurrentScoreFails() {
        StudentScore current = ScoreChangeRequestTestFixtures.score(
                30L, COLUMN_ID, STUDENT_ID, ScoreStatus.SCORED, new BigDecimal("8.0"));

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> validator.validateDifferentFromCurrent(
                        Optional.of(current), ScoreStatus.SCORED, new BigDecimal("8.0")));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(), "unchanged score should be rejected");
    }

    @Test
    void snapshotMismatchFails() {
        com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequest request =
                ScoreChangeRequestTestFixtures.request(
                40L, COLUMN_ID, STUDENT_ID, 100L,
                ScoreSnapshotStatus.SCORED, new BigDecimal("7.0"),
                ScoreStatus.SCORED, new BigDecimal("8.0"));
        StudentScore current = ScoreChangeRequestTestFixtures.score(
                300L, COLUMN_ID, STUDENT_ID, ScoreStatus.SCORED, new BigDecimal("7.5"));

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> validator.validateSnapshotMatch(request, Optional.of(current)));

        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(), "changed snapshot should conflict");
    }
}
