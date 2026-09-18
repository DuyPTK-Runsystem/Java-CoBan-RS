package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogScheduleItemResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;

import lombok.RequiredArgsConstructor;

/** Builds schedule and unlogged entry response records. */
@Service
@RequiredArgsConstructor
public class LessonLogScheduleEntryFactory {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LessonLogScheduleCalendarCatalog calendarCatalog;
    private final LessonLogScheduleNameResolver nameResolver;
    private final LessonLogPolicyService policyService;

    public LessonLogScheduleItemResponse scheduleItem(TimetableEntry timetableEntry, TimetableRevision revision,
            SubjectTeachingAssignment assignment, ClassSubject classSubject, Semester semester, TimetablePeriod period,
            LessonLogEntry logged, LocalDate date, Function<LessonLogEntry, LessonLogEntryResponse> mapper,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter) {
        LocalDateTime lessonEndsAt = LocalDateTime.of(date, period.getEndTime());
        LessonLogPolicy policy = policyService.findEffectivePolicy(date).orElse(null);
        LocalDateTime expiresAt = policy == null ? null : policyService.editWindowExpiresAt(policy, lessonEndsAt);
        boolean closed = calendarCatalog.closedOn(semester.getId(), date);
        ScheduleValues values = scheduleValues(logged, policy, semester, closed, lessonEndsAt, expiresAt,
                classSubject, mapper, rosterCounter);
        return new LessonLogScheduleItemResponse(values.id(), timetableEntry.getId(),
                revision.getId(), assignment.getId(), classSubject.getClassId(), classSubject.getSubjectId(),
                assignment.getTeacherId(), semester.getId(), nameResolver.className(classSubject),
                nameResolver.subjectName(classSubject), nameResolver.teacherName(assignment), nameResolver.roomName(timetableEntry),
                date, period.getSession().name(), period.getPeriodIndex(),
                lessonEndsAt, values.editWindowExpiresAt(), values.status(), values.version(), values.policyId(),
                values.rosterCount(), values.canWrite(), values.canLateRecord(), values.blockedReason(),
                values.response());
    }

    private ScheduleValues scheduleValues(LessonLogEntry logged, LessonLogPolicy policy, Semester semester,
            boolean closed, LocalDateTime lessonEndsAt, LocalDateTime expiresAt,
            ClassSubject classSubject, Function<LessonLogEntry, LessonLogEntryResponse> mapper,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter) {
        return new ScheduleValues(logged == null ? 0L : logged.getId(),
                logged == null ? expiresAt : logged.getEditWindowExpiresAt(),
                logged == null ? LessonLogStatus.UNLOGGED : logged.getStatus(),
                logged == null ? 0L : logged.getVersion(), policyId(logged),
                rosterCount(logged, classSubject, lessonEndsAt, rosterCounter),
                canWrite(semester, closed, lessonEndsAt, expiresAt) && logged == null,
                lateRecordAllowed(logged, policy, semester, closed, expiresAt),
                blockedReason(logged, closed, lessonEndsAt, expiresAt, semester),
                logged == null ? null : mapper.apply(logged));
    }

    private Long policyId(LessonLogEntry logged) {
        return logged == null ? null : logged.getPolicyId();
    }

    private int rosterCount(LessonLogEntry logged, ClassSubject classSubject, LocalDateTime lessonEndsAt,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter) {
        return logged == null ? rosterCounter.applyAsInt(classSubject.getClassId(), lessonEndsAt)
                : logged.getRosterCountSnapshot();
    }

    private boolean lateRecordAllowed(LessonLogEntry logged, LessonLogPolicy policy, Semester semester,
            boolean closed, LocalDateTime expiresAt) {
        return LessonLogActorAuthorization.isManager() && logged == null && policy != null && expiresAt != null
                && !currentTime().isBefore(expiresAt)
                && semesterOpen(semester) && !closed;
    }

    public LessonLogEntryResponse unlogged(TimetableEntry timetableEntry, TimetableRevision revision,
            SubjectTeachingAssignment assignment, ClassSubject classSubject, Semester semester, TimetablePeriod period,
            LocalDate date, ToIntBiFunction<Long, LocalDateTime> rosterCounter,
            Function<LessonLogPolicy, List<Object>> rubricMapper) {
        LocalDateTime lessonEndsAt = LocalDateTime.of(date, period.getEndTime());
        LessonLogPolicy policy = policyService.findEffectivePolicy(date).orElse(null);
        LocalDateTime expiresAt = policy == null ? null : policyService.editWindowExpiresAt(policy, lessonEndsAt);
        boolean canLateRecord = LessonLogActorAuthorization.isManager()
                && expiresAt != null && !currentTime().isBefore(expiresAt)
                && semesterOpen(semester) && !calendarClosed(semester, date);
        String blockedReason = unloggedBlockedReason(policy, semester, date, expiresAt);
        return new LessonLogEntryResponse(0L, timetableEntry.getId(), revision.getId(), assignment.getId(), semester.getId(),
                date, classSubject.getClassId(), classSubject.getSubjectId(), assignment.getTeacherId(),
                nameResolver.className(classSubject), nameResolver.subjectName(classSubject),
                nameResolver.teacherName(assignment), nameResolver.roomName(timetableEntry), period.getSession().name(),
                period.getPeriodIndex(), lessonEndsAt, expiresAt, null, null, null, null, null, null, null, null, null,
                LessonLogStatus.UNLOGGED, 0L, policy == null ? null : policy.getId(), rubricMapper.apply(policy),
                rosterCounter.applyAsInt(classSubject.getClassId(), lessonEndsAt), false, false, false, false, canLateRecord,
                blockedReason, null, null, null, null, null);
    }

    private boolean canWrite(Semester semester, boolean closed, LocalDateTime lessonEndsAt, LocalDateTime expiresAt) {
        return semesterOpen(semester) && !closed && !lessonEndsAt.isAfter(currentTime()) && expiresAt != null
                && currentTime().isBefore(expiresAt);
    }

    private boolean semesterOpen(Semester semester) {
        return semester.getStatus() != SemesterStatus.CLOSED && semester.getStatus() != SemesterStatus.LOCKED;
    }

    private boolean calendarClosed(Semester semester, LocalDate date) {
        return calendarCatalog.closedOn(semester.getId(), date);
    }

    private String unloggedBlockedReason(LessonLogPolicy policy, Semester semester, LocalDate date,
            LocalDateTime expiresAt) {
        if (policy == null || expiresAt == null) {
            return "Chưa cấu hình policy sổ đầu bài";
        }
        if (!semesterOpen(semester)) {
            return "Học kỳ đã đóng";
        }
        if (calendarClosed(semester, date)) {
            return "Ngày học đã đóng";
        }
        if (currentTime().isBefore(expiresAt)) {
            return "Chưa đến hạn ghi bổ sung";
        }
        return LessonLogActorAuthorization.isManager() ? null : "Cần giáo vụ ghi bổ sung";
    }

    private String blockedReason(LessonLogEntry logged, boolean closed, LocalDateTime lessonEndsAt,
            LocalDateTime expiresAt, Semester semester) {
        if (logged != null) {
            return null;
        }
        if (closed) {
            return "Ngày học đã đóng";
        }
        if (lessonEndsAt.isAfter(currentTime())) {
            return "Tiết chưa kết thúc";
        }
        if (expiresAt == null) {
            return "Chưa cấu hình policy sổ đầu bài";
        }
        if (!currentTime().isBefore(expiresAt)) {
            return LessonLogActorAuthorization.isManager() ? null : "Cần giáo vụ ghi bổ sung";
        }
        return semesterOpen(semester) ? null : "Học kỳ đã đóng";
    }

    private LocalDateTime currentTime() {
        return LocalDateTime.now(ZONE);
    }

    private record ScheduleValues(Long id, LocalDateTime editWindowExpiresAt, LessonLogStatus status, Long version,
            Long policyId, int rosterCount, boolean canWrite, boolean canLateRecord, String blockedReason,
            LessonLogEntryResponse response) {
    }
}
