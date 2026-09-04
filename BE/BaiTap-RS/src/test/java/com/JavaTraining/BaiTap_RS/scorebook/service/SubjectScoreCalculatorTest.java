package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals",
        "PMD.TooManyMethods",
        "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class SubjectScoreCalculatorTest {

    private final SubjectScoreCalculator calculator = new SubjectScoreCalculator();

    @Test
    void calculatesNormalSubjectWithAllAssessmentColumnsAndRoundsHalfUp() {
        List<AssessmentColumn> columns = List.of(
                column(1L, AssessmentType.KTTT, 1),
                column(2L, AssessmentType.KTTT, 2),
                column(3L, AssessmentType.KTDK, 1),
                column(4L, AssessmentType.KTCK, 1));
        List<StudentScore> scores = List.of(
                score(1L, "8.0", ScoreStatus.SCORED),
                score(2L, "9.0", ScoreStatus.SCORED),
                score(3L, "7.0", ScoreStatus.SCORED),
                score(4L, "6.2", ScoreStatus.SCORED));

        BigDecimal result = calculator.calculateNormalSubjectTermScore(columns, scores);

        Assertions.assertEquals(new BigDecimal("7.1"), result);
    }

    @Test
    void calculatesZeroAsAValidScoredValue() {
        List<AssessmentColumn> columns = List.of(
                column(1L, AssessmentType.KTTT, 1),
                column(2L, AssessmentType.KTDK, 1));
        List<StudentScore> scores = List.of(
                score(1L, "0.0", ScoreStatus.SCORED),
                score(2L, "8.0", ScoreStatus.SCORED));

        BigDecimal result = calculator.calculateNormalSubjectTermScore(columns, scores);

        Assertions.assertEquals(new BigDecimal("5.3"), result);
    }

    @Test
    void ignoresAbsentExemptedCancelledAndNullScores() {
        List<AssessmentColumn> columns = List.of(
                column(1L, AssessmentType.KTTT, 1),
                column(2L, AssessmentType.KTDK, 1),
                column(3L, AssessmentType.KTCK, 1),
                column(4L, AssessmentType.KTTT, 2));
        List<StudentScore> scores = List.of(
                score(1L, "8.0", ScoreStatus.SCORED),
                score(2L, "7.0", ScoreStatus.SCORED),
                score(3L, null, ScoreStatus.ABSENT),
                score(4L, "9.0", ScoreStatus.CANCELLED));

        BigDecimal result = calculator.calculateNormalSubjectTermScore(columns, scores);

        Assertions.assertEquals(new BigDecimal("7.3"), result);
    }

    @Test
    void returnsNullWhenNoNormalScoreIsValid() {
        List<AssessmentColumn> columns = List.of(column(1L, AssessmentType.KTTT, 1));
        List<StudentScore> scores = List.of(score(1L, null, ScoreStatus.EXEMPTED));

        Assertions.assertNull(calculator.calculateNormalSubjectTermScore(columns, scores));
    }

    @Test
    void calculatesSkillSubjectUsingConfiguredPercentages() {
        SkillWeightConfig config = new SkillWeightConfig(
                90L,
                new BigDecimal("20"),
                new BigDecimal("30"),
                new BigDecimal("50"),
                7L,
                LocalDateTime.now());
        List<AssessmentColumn> columns = List.of(
                column(1L, AssessmentType.KTTT, 1),
                column(2L, AssessmentType.KTDK, 1),
                column(3L, AssessmentType.KTCK, 1));
        List<StudentScore> scores = List.of(
                score(1L, "7.0", ScoreStatus.SCORED),
                score(2L, "8.0", ScoreStatus.SCORED),
                score(3L, "9.0", ScoreStatus.SCORED));

        BigDecimal result = calculator.calculateSkillSubjectTermScore(config, columns, scores);

        Assertions.assertEquals(new BigDecimal("8.3"), result);
    }

    @Test
    void returnsNullForSkillSubjectWithoutWeightConfiguration() {
        Assertions.assertNull(calculator.calculateSkillSubjectTermScore(
                null,
                List.of(column(1L, AssessmentType.KTTT, 1)),
                List.of(score(1L, "8.0", ScoreStatus.SCORED))));
    }

    @Test
    void calculatesTermAverageFromAcademicScoresOnly() {
        BigDecimal result = calculator.calculateTermAverage(Arrays.asList(
                new BigDecimal("8.1"),
                null,
                new BigDecimal("9.2")));

        Assertions.assertEquals(new BigDecimal("8.7"), result);
    }

    @Test
    void returnsNullForEmptyTermAverage() {
        Assertions.assertNull(calculator.calculateTermAverage(List.of()));
    }

    @Test
    void calculatesFullYearSubjectWithDoubleWeightForSecondSemester() {
        BigDecimal result = calculator.calculateAnnualSubjectScore(
                new BigDecimal("7.0"), new BigDecimal("8.0"), true);

        Assertions.assertEquals(new BigDecimal("7.7"), result);
    }

    @Test
    void returnsNullForFullYearSubjectMissingOneSemester() {
        Assertions.assertNull(calculator.calculateAnnualSubjectScore(
                new BigDecimal("7.0"), null, true));
    }

    @Test
    void returnsTheOnlyAvailableSemesterForOneSemesterSubject() {
        Assertions.assertEquals(
                new BigDecimal("7.5"),
                calculator.calculateAnnualSubjectScore(null, new BigDecimal("7.5"), false));
    }

    @Test
    void calculatesAnnualAverageAndIgnoresMissingSubjects() {
        BigDecimal result = calculator.calculateAnnualAverage(Arrays.asList(
                new BigDecimal("8.0"),
                null,
                new BigDecimal("9.0")));

        Assertions.assertEquals(new BigDecimal("8.5"), result);
    }

    private static AssessmentColumn column(Long id, AssessmentType type, int columnNo) {
        AssessmentColumn column = new AssessmentColumn(
                90L,
                type,
                columnNo,
                type.name(),
                type.standardWeight(),
                type.isRequiredByStructure());
        ReflectionTestUtils.setField(column, "id", id);
        column.setStatus(AssessmentColumnStatus.ACTIVE);
        return column;
    }

    private static StudentScore score(Long columnId, String value, ScoreStatus status) {
        return new StudentScore(
                columnId,
                200L,
                status,
                value == null ? null : new BigDecimal(value),
                null,
                7L);
    }
}
