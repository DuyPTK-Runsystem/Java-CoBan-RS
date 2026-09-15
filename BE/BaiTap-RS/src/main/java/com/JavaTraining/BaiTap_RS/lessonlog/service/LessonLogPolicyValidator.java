package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqPolicyDTO;

/** Validates the independent business rules of a lesson-log policy request. */
@Component
public class LessonLogPolicyValidator {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> DEADLINE_MODES = Set.of("FIXED_HOURS", "END_OF_WEEK");

    public void validate(ReqPolicyDTO request) {
        validateEffectiveDate(request.effectiveFrom());
        validateDeadlineMode(request.deadlineMode());
        validateEditWindow(request.deadlineMode(), request.editWindowHours());
        validateTimezone(request.timezone());
    }

    private void validateEffectiveDate(LocalDate effectiveFrom) {
        if (effectiveFrom.isBefore(LocalDate.now(ZONE))) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Policy chỉ có hiệu lực từ ngày tương lai");
        }
    }

    private void validateDeadlineMode(String deadlineMode) {
        if (!DEADLINE_MODES.contains(deadlineMode)) {
            throw error(HttpStatus.BAD_REQUEST, "deadlineMode không hợp lệ");
        }
    }

    private void validateEditWindow(String deadlineMode, Integer editWindowHours) {
        if ("FIXED_HOURS".equals(deadlineMode)
                && (editWindowHours == null || editWindowHours < 1 || editWindowHours > 168)) {
            throw error(HttpStatus.BAD_REQUEST, "editWindowHours phải từ 1 đến 168");
        }
        if ("END_OF_WEEK".equals(deadlineMode) && editWindowHours != null) {
            throw error(HttpStatus.BAD_REQUEST, "END_OF_WEEK không nhận editWindowHours");
        }
    }

    private void validateTimezone(String timezone) {
        if (!ZONE.getId().equals(timezone)) {
            throw error(HttpStatus.BAD_REQUEST, "timezone phải là Asia/Ho_Chi_Minh");
        }
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}
