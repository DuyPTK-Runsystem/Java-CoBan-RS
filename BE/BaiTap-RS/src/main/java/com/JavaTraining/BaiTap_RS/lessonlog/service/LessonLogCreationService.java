package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqCreateLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqLateRecordLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntrySourceData;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogCreationService {
    private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LessonLogEntryRepository entries;
    private final LessonLogPolicyService policyService;
    private final LessonLogWeeklyReviewService weeklyReviewService;
    private final LessonLogAuditService auditService;
    private final LessonLogEntryEditor entryEditor;
    private final LessonLogSourceResolver sourceResolver;
    private final TeacherRepository teachers;
    private final LessonLogRosterService rosterService;
    private final LessonLogSnapshotService snapshotService;

    @Transactional
    public LessonLogEntry create(ReqCreateLessonLogDTO request) {
        LessonLogSource source = sourceResolver.resolve(request.timetableEntryId(), request.lessonDate());
        LessonLogActorAuthorization.assertTeacherAssignment(source.assignment().getTeacherId(), teachers);
        String session = source.period().getSession().name();
        assertEntryDoesNotExist(source, request.lessonDate(), session, "Tiết đã có sổ");
        LessonLogPolicy policy = policyService.getEffectivePolicy(request.lessonDate());
        LocalDateTime lessonEnd = policyService.lessonEndsAt(request.lessonDate(), source.period());
        LocalDateTime editExpiry = policyService.editWindowExpiresAt(policy, lessonEnd);
        assertWritable(source.semester(), lessonEnd, editExpiry);
        int rosterSnapshot = rosterService.count(source.classId(), lessonEnd);
        LessonLogEntry entry = newEntry(source, policy, request.lessonDate(), session, lessonEnd, editExpiry, rosterSnapshot);
        entryEditor.apply(entry, new LessonLogEntryContent(request.title(), request.content(), request.completionStatus(),
                request.grade(), request.presentCount(), request.absentCount(), request.comments(),
                request.absentStudentNotes(), request.homework()));
        entry = entries.save(entry);
        auditService.record(entry, "CREATE", null);
        weeklyReviewService.ensureExists(entry);
        return entry;
    }

    @Transactional
    public LessonLogEntry lateRecord(ReqLateRecordLessonLogDTO request) {
        LessonLogActorAuthorization.assertManager();
        LessonLogSource source = sourceResolver.resolve(request.timetableEntryId(), request.lessonDate());
        String session = source.period().getSession().name();
        assertEntryDoesNotExist(source, request.lessonDate(), session, "Tiết này đã có sổ đầu bài");
        LessonLogPolicy policy = policyService.getEffectivePolicy(request.lessonDate());
        LocalDateTime lessonEnd = policyService.lessonEndsAt(request.lessonDate(), source.period());
        LocalDateTime editExpiry = policyService.editWindowExpiresAt(policy, lessonEnd);
        assertOpenSemester(source.semester());
        if (now().isBefore(editExpiry)) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Chưa đến thời điểm ghi bổ sung");
        }
        int rosterSnapshot = rosterService.count(source.classId(), lessonEnd);
        LessonLogEntry entry = newEntry(source, policy, request.lessonDate(), session, lessonEnd, editExpiry, rosterSnapshot);
        entryEditor.apply(entry, new LessonLogEntryContent(request.title(), request.content(), request.completionStatus(),
                request.grade(), request.presentCount(), request.absentCount(), request.comments(),
                request.absentStudentNotes(), request.homework()));
        entryEditor.validateComplete(entry);
        entry.setStatus(LessonLogStatus.AMENDED);
        entry = entries.save(entry);
        auditService.record(entry, new LessonLogAuditChange("LATE_RECORD", request.reason(), null, auditService.state(entry)));
        weeklyReviewService.ensureExists(entry);
        weeklyReviewService.invalidate(entry);
        return entry;
    }

    private LessonLogEntry newEntry(LessonLogSource source, LessonLogPolicy policy, java.time.LocalDate lessonDate,
            String session, LocalDateTime lessonEnd, LocalDateTime editExpiry, int rosterSnapshot) {
        LessonLogEntry entry = LessonLogEntry.create(new LessonLogEntrySourceData(source.timetable().getId(),
                source.revision().getId(), source.assignment().getId(), source.semester().getId(), source.classId(),
                source.subjectId(), source.assignment().getTeacherId(), policy.getId(), lessonDate, session,
                source.period().getPeriodIndex(), snapshotService.create(source, policy, lessonDate, rosterSnapshot),
                lessonEnd, editExpiry, actor()));
        entry.setRosterCountSnapshot(rosterSnapshot);
        return entry;
    }

    private void assertEntryDoesNotExist(LessonLogSource source, java.time.LocalDate lessonDate, String session,
            String conflictMessage) {
        if (entries.findByClassIdAndLessonDateAndSessionAndPeriodIndex(source.classId(), lessonDate, session,
                source.period().getPeriodIndex()).isPresent()) {
            throw error(HttpStatus.CONFLICT, conflictMessage);
        }
    }

    private void assertWritable(Semester semester, LocalDateTime lessonEnd, LocalDateTime editExpiry) {
        assertOpenSemester(semester);
        if (lessonEnd.isAfter(now()) || !now().isBefore(editExpiry)) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Tiết chưa kết thúc hoặc đã hết hạn chỉnh sửa");
        }
    }

    private void assertOpenSemester(Semester semester) {
        if (semester.getStatus() == SemesterStatus.CLOSED || semester.getStatus() == SemesterStatus.LOCKED) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Học kỳ đã đóng");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(TIME_ZONE);
    }

    private Long actor() {
        return AuditContext.currentUserId();
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }

}
