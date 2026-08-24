package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertSkillWeightConfigDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ScorebookConfigurationValidator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    public void ensureWritable(Scorebook scorebook) {
        ensureStatus(scorebook, ScorebookStatus.OPEN, "Chỉ sổ điểm OPEN mới được cấu hình");
    }

    public void ensureOpen(Scorebook scorebook) {
        ensureStatus(scorebook, ScorebookStatus.OPEN, "Chỉ sổ điểm OPEN mới được publish");
    }

    public void ensureDraft(Scorebook scorebook) {
        ensureStatus(scorebook, ScorebookStatus.DRAFT, "Chỉ sổ điểm DRAFT mới được mở");
    }

    public void validatePublishColumns(Subject subject, List<AssessmentColumn> columns) {
        long continuous = count(columns, AssessmentType.KTTT);
        long periodic = count(columns, AssessmentType.KTDK);
        long finalColumns = count(columns, AssessmentType.KTCK);
        boolean valid = subject.getSubjectType() == SubjectType.SKILL
                ? continuous == 1 && periodic == 1 && finalColumns == 1
                : periodic >= 1 && finalColumns == 1;
        if (!valid) {
            throw conflict(subject.getSubjectType() == SubjectType.SKILL
                    ? "Môn kỹ năng phải có đúng ba cột KTTT, KTĐK và KTCK"
                    : "Môn thường phải có ít nhất một cột KTĐK và đúng một cột KTCK");
        }
    }

    public void validateWeights(ReqUpsertSkillWeightConfigDTO request) {
        validateWeights(request.ktttWeightPercent(), request.ktdkWeightPercent(), request.ktckWeightPercent());
    }

    public void validateWeights(BigDecimal kttt, BigDecimal ktdk, BigDecimal ktck) {
        if (hasInvalidPercent(kttt) || hasInvalidPercent(ktdk) || hasInvalidPercent(ktck)) {
            throw conflict("Trọng số môn kỹ năng không hợp lệ");
        }
        BigDecimal total = kttt.add(ktdk).add(ktck);
        if (total.compareTo(ONE_HUNDRED) != 0 || ktck.compareTo(kttt) < 0 || ktck.compareTo(ktdk) < 0) {
            throw conflict("Trọng số môn kỹ năng không hợp lệ");
        }
    }

    private long count(List<AssessmentColumn> columns, AssessmentType type) {
        return columns.stream().filter(column -> column.getAssessmentType() == type).count();
    }

    private boolean hasInvalidPercent(BigDecimal value) {
        return value == null || value.signum() < 0 || value.compareTo(ONE_HUNDRED) > 0;
    }

    private void ensureStatus(Scorebook scorebook, ScorebookStatus expected, String message) {
        if (scorebook.getStatus() != expected) {
            throw conflict(message);
        }
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
