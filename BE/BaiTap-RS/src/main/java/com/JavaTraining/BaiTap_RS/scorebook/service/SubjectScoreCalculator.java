package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import org.springframework.stereotype.Component;

/**
 * Pure calculation rules for scorebook-derived results.
 *
 * <p>Only SCORED values are considered. Missing or exceptional score cells do
 * not become zeroes.</p>
 */
@Component
public class SubjectScoreCalculator {

    private static final int SCALE = 1;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public BigDecimal calculateNormalSubjectTermScore(
            List<AssessmentColumn> columns, List<StudentScore> scores) {
        Map<Long, StudentScore> scoresByColumn = indexScores(scores);
        BigDecimal weightedTotal = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (AssessmentColumn column : columns) {
            StudentScore score = scoresByColumn.get(column.getId());
            if (!isScored(score)) {
                continue;
            }
            BigDecimal weight = column.getWeightFactor() == null
                    ? column.getAssessmentType().standardWeight()
                    : column.getWeightFactor();
            weightedTotal = weightedTotal.add(score.getScoreValue().multiply(weight));
            totalWeight = totalWeight.add(weight);
        }
        return totalWeight.signum() == 0 ? null : round(weightedTotal.divide(totalWeight, 6, RoundingMode.HALF_UP));
    }

    public BigDecimal calculateSkillSubjectTermScore(
            SkillWeightConfig config, List<AssessmentColumn> columns, List<StudentScore> scores) {
        if (config == null) {
            return null;
        }
        Map<Long, StudentScore> scoresByColumn = indexScores(scores);
        BigDecimal kttt = scoreForType(AssessmentType.KTTT, columns, scoresByColumn);
        BigDecimal ktdk = scoreForType(AssessmentType.KTDK, columns, scoresByColumn);
        BigDecimal ktck = scoreForType(AssessmentType.KTCK, columns, scoresByColumn);
        if (kttt == null || ktdk == null || ktck == null) {
            return null;
        }
        BigDecimal weighted = kttt.multiply(config.getKtttWeightPercent())
                .add(ktdk.multiply(config.getKtdkWeightPercent()))
                .add(ktck.multiply(config.getKtckWeightPercent()));
        return round(weighted.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
    }

    public BigDecimal calculateTermAverage(List<BigDecimal> normalSubjectScores) {
        return average(normalSubjectScores);
    }

    public BigDecimal calculateAnnualSubjectScore(
            BigDecimal hk1Score, BigDecimal hk2Score, boolean isFullYear) {
        if (!isFullYear) {
            return hk1Score != null ? hk1Score : hk2Score;
        }
        if (hk1Score == null || hk2Score == null) {
            return null;
        }
        return round(hk1Score.add(hk2Score.multiply(BigDecimal.valueOf(2)))
                .divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP));
    }

    public BigDecimal calculateAnnualAverage(List<BigDecimal> normalAnnualSubjectScores) {
        return average(normalAnnualSubjectScores);
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> presentValues = values == null
                ? List.of()
                : values.stream().filter(value -> value != null).toList();
        if (presentValues.isEmpty()) {
            return null;
        }
        BigDecimal total = presentValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return round(total.divide(BigDecimal.valueOf(presentValues.size()), 6, RoundingMode.HALF_UP));
    }

    private BigDecimal scoreForType(
            AssessmentType type,
            List<AssessmentColumn> columns,
            Map<Long, StudentScore> scoresByColumn) {
        return columns.stream()
                .filter(column -> column.getAssessmentType() == type)
                .map(column -> scoresByColumn.get(column.getId()))
                .filter(this::isScored)
                .map(StudentScore::getScoreValue)
                .findFirst()
                .orElse(null);
    }

    private Map<Long, StudentScore> indexScores(List<StudentScore> scores) {
        if (scores == null) {
            return Map.of();
        }
        return scores.stream().collect(Collectors.toMap(
                StudentScore::getAssessmentColumnId,
                Function.identity(),
                (first, ignored) -> first));
    }

    private boolean isScored(StudentScore score) {
        return score != null
                && score.getScoreStatus() == ScoreStatus.SCORED
                && score.getScoreValue() != null;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
