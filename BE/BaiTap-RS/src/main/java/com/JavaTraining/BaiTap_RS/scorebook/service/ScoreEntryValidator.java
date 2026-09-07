package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqBulkScoreItemDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Validate business rules cho việc nhập/sửa điểm học sinh.
 */
@Component
public class ScoreEntryValidator {

    private static final BigDecimal SCORE_MIN = BigDecimal.ZERO;
    private static final BigDecimal SCORE_MAX = BigDecimal.TEN;
    private static final long DIRECT_EDIT_DAYS = 10;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public void validateScoreValue(ScoreStatus status, BigDecimal value) {
        if (status == ScoreStatus.SCORED) {
            validateScoredValue(value);
        } else if (value != null) {
            throw badRequest("Trạng thái " + status + " không được có giá trị điểm");
        }
    }

    private void validateScoredValue(BigDecimal value) {
        if (value == null) {
            throw badRequest("Điểm SCORED phải có giá trị");
        }
        if (value.compareTo(SCORE_MIN) < 0 || value.compareTo(SCORE_MAX) > 0) {
            throw badRequest("Điểm phải nằm trong khoảng 0.0 đến 10.0");
        }
        if (value.scale() > 1) {
            throw badRequest("Điểm tối đa 1 chữ số thập phân");
        }
    }

    public void validateUpdateEligibility(StudentScore existing, Semester semester) {
        LocalDate enteredDate = existing.getEnteredAt().atZone(BUSINESS_ZONE).toLocalDate();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        long daysSinceEntry = ChronoUnit.DAYS.between(enteredDate, today);
        if (daysSinceEntry > DIRECT_EDIT_DAYS) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Quá thời hạn " + DIRECT_EDIT_DAYS + " ngày để sửa trực tiếp. "
                            + "Vui lòng tạo yêu cầu sửa điểm");
        }
        if (semester.getStatus() == SemesterStatus.LOCKED
                || semester.getStatus() == SemesterStatus.CLOSED) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Không thể sửa điểm khi học kỳ đã khóa hoặc đóng");
        }
    }

    public void validateVersion(StudentScore existing, Long expectedVersion) {
        if (expectedVersion == null) {
            throw badRequest("Cập nhật điểm phải có expectedVersion");
        }
        if (!existing.getVersion().equals(expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Xung đột phiên bản: phiên bản hiện tại là "
                            + existing.getVersion() + ", yêu cầu là " + expectedVersion);
        }
    }

    public void validateCreateVersion(Long expectedVersion) {
        if (expectedVersion != null) {
            throw badRequest("Tạo mới điểm không cần expectedVersion");
        }
    }

    public void validateNoDuplicateStudents(List<ReqBulkScoreItemDTO> items) {
        Set<Long> seen = new HashSet<>();
        for (ReqBulkScoreItemDTO item : items) {
            if (!seen.add(item.studentId())) {
                throw badRequest("Trùng lặp học sinh trong danh sách: " + item.studentId());
            }
        }
    }

    private AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, message);
    }
}
