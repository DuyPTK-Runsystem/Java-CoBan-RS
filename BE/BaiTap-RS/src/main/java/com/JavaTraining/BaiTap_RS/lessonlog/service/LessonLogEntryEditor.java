package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;

@Component
public class LessonLogEntryEditor {
    private static final Set<String> GRADES = Set.of("A", "B", "C", "D");
    private static final Set<String> COMPLETION_STATUSES = Set.of("ON_SCHEDULE", "BEHIND_SCHEDULE", "AHEAD_OF_SCHEDULE");

    public void apply(LessonLogEntry entry, LessonLogEntryContent content) {
        entry.setTitle(content.title() == null ? null : content.title().trim()); entry.setContent(content.content());
        entry.setCompletionStatus(content.completionStatus()); entry.setGrade(content.grade());
        entry.setPresentCount(content.presentCount()); entry.setAbsentCount(content.absentCount());
        entry.setComments(content.comments()); entry.setAbsentStudentNotes(content.absentStudentNotes());
        entry.setHomework(content.homework());
    }

    public void validateComplete(LessonLogEntry entry) {
        if (!hasRequiredFields(entry)) {
            throw error("Sổ chưa đủ thông tin bắt buộc");
        }
        if (entry.getRosterCountSnapshot() != null
                && entry.getPresentCount() + entry.getAbsentCount() != entry.getRosterCountSnapshot()) {
            throw error("Sĩ số không khớp tổng");
        }
        if (!GRADES.contains(entry.getGrade()) || !COMPLETION_STATUSES.contains(entry.getCompletionStatus())) {
            throw error("Giá trị đánh giá không hợp lệ");
        }
    }

    public boolean hasRequiredFields(LessonLogEntry entry) {
        return entry.getTitle() != null && !entry.getTitle().isBlank() && entry.getCompletionStatus() != null
                && entry.getGrade() != null && entry.getPresentCount() != null && entry.getAbsentCount() != null;
    }

    private AppException error(String message) { return new AppException(HttpStatus.UNPROCESSABLE_ENTITY, message); }
}
