package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.*;
import java.util.*;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.*;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.*;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.*;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.*;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.*;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class LessonLogService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LessonLogEntryRepository entries;
    private final LessonLogPolicyService policyService;
    private final LessonLogWeeklyReviewService weeklyReviewService;
    private final LessonLogWeeklyCommandService weeklyCommandService;
    private final LessonLogAuditService auditService;
    private final LessonLogEntryEditor entryEditor;
    // default access modifier retained for legacy test wiring
    LessonLogEntryLifecycleService lifecycleService;
    private final LessonLogEntryCommandService entryCommandService;
    private final LessonLogQueryService queryService;
    private final LessonLogScheduleService scheduleService;
    private final LessonLogSourceResolver sourceResolver;
    private final SemesterRepository semesters;
    private final SchoolClassRepository schoolClasses;
    private final TeacherRepository teachers;
    private final SubjectRepository subjects;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;
    @Transactional
    public LessonLogEntryResponse create(ReqCreateLessonLogDTO r) {
        LessonLogSource s = sourceResolver.resolve(r.timetableEntryId(), r.lessonDate());
        LessonLogActorAuthorization.assertTeacherAssignment(s.assignment().getTeacherId(), teachers);
        String session = s.period().getSession().name();
        if (entries.findByClassIdAndLessonDateAndSessionAndPeriodIndex(s.classId(), r.lessonDate(), session,
                s.period().getPeriodIndex()).isPresent()) {
            throw err(HttpStatus.CONFLICT, "Tiết này đã có sổ đầu bài");
        }
        LessonLogPolicy p = policyService.getEffectivePolicy(r.lessonDate());
        LocalDateTime end = policyService.lessonEndsAt(r.lessonDate(), s.period());
        LocalDateTime expiry = policyService.editWindowExpiresAt(p, end);
        writable(s.semester(), end, expiry);
        int rosterSnapshot = roster(s.classId(), end);
        LessonLogEntry e = LessonLogEntry.create(new LessonLogEntrySourceData(s.timetable().getId(), s.revision().getId(),
                s.assignment().getId(), s.semester().getId(), s.classId(), s.subjectId(), s.assignment().getTeacherId(),
                p.getId(), r.lessonDate(), session, s.period().getPeriodIndex(), snapshot(s, p, r.lessonDate(), rosterSnapshot),
                end, expiry, actor()));
        e.setRosterCountSnapshot(rosterSnapshot);
        entryEditor.apply(e, new LessonLogEntryContent(r.title(), r.content(), r.completionStatus(), r.grade(),
                r.presentCount(), r.absentCount(), r.comments(), r.absentStudentNotes(), r.homework()));
        e = entries.save(e);
        auditService.record(e, "CREATE", null);
        weeklyReviewService.ensureExists(e);
        return map(e);
    }

    @Transactional(readOnly = true)
    public LessonLogEntryResponse get(Long id) {
        return queryService.get(id);
    }

    @Transactional
    public LessonLogEntryResponse update(Long id, ReqUpdateLessonLogDTO r) {
        return entryCommandService.update(id, r, this::map);
    }

    @Transactional
    public LessonLogEntryResponse submit(Long id, Long v) {
        return entryCommandService.submit(id, v, this::map);
    }

    @Transactional
    public LessonLogEntryResponse review(Long id, ReqTransitionLessonLogDTO r) {
        return entryCommandService.review(id, r, this::map);
    }

    @Transactional
    public LessonLogEntryResponse amend(Long id, ReqTransitionLessonLogDTO r) {
        return entryCommandService.amend(id, r, this::map);
    }

    @Transactional
    public LessonLogEntryResponse lateRecord(ReqLateRecordLessonLogDTO r) {
        LessonLogSource s = sourceResolver.resolve(r.timetableEntryId(), r.lessonDate());
        String session = s.period().getSession().name();
        if (entries.findByClassIdAndLessonDateAndSessionAndPeriodIndex(s.classId(), r.lessonDate(), session,
                s.period().getPeriodIndex()).isPresent()) {
            throw err(HttpStatus.CONFLICT, "Tiết đã có sổ");
        }
        LessonLogPolicy p = policyService.getEffectivePolicy(r.lessonDate());
        LocalDateTime end = policyService.lessonEndsAt(r.lessonDate(), s.period());
        LocalDateTime ex = policyService.editWindowExpiresAt(p, end);
        assertOpenSemester(s.semester());
        if (now().isBefore(ex)) {
            throw err(HttpStatus.UNPROCESSABLE_ENTITY, "Chưa đến thời điểm ghi bổ sung");
        }
        int rosterSnapshot = roster(s.classId(), end);
        LessonLogEntry e = LessonLogEntry.create(new LessonLogEntrySourceData(s.timetable().getId(), s.revision().getId(),
                s.assignment().getId(), s.semester().getId(), s.classId(), s.subjectId(), s.assignment().getTeacherId(),
                p.getId(), r.lessonDate(), session, s.period().getPeriodIndex(), snapshot(s, p, r.lessonDate(), rosterSnapshot),
                end, ex, actor()));
        e.setRosterCountSnapshot(rosterSnapshot);
        entryEditor.apply(e, new LessonLogEntryContent(r.title(), r.content(), r.completionStatus(), r.grade(),
                r.presentCount(), r.absentCount(), r.comments(), r.absentStudentNotes(), r.homework()));
        entryEditor.validateComplete(e);
        e.setStatus(LessonLogStatus.AMENDED);
        e = entries.save(e);
        auditService.record(e, new LessonLogAuditChange("LATE_RECORD", r.reason(), null, auditService.state(e)));
        weeklyReviewService.ensureExists(e);
        weeklyReviewService.invalidate(e);
        return map(e);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevision> revisions(Long id, int page, int size) {
        return queryService.revisions(id, page, size);
    }

    @Transactional(readOnly = true)
    public LessonLogScheduleResponse mySchedule(LocalDate date) {
        return queryService.mySchedule(date);
    }

    @Transactional(readOnly = true)
    public List<LessonLogClassResponse> classes(Long semesterId) {
        return queryService.classes(semesterId);
    }

    @Transactional(readOnly = true)
    public LessonLogClassWeekResponse classWeek(Long classId, Long semesterId, LocalDate weekStart) {
        return queryService.classWeek(classId, semesterId, weekStart);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevisionResponse> weeklyRevisions(Long classId, Long semesterId,
            LocalDate weekStart, int page, int size) {
        return queryService.weeklyRevisions(classId, semesterId, weekStart, page, size);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevisionResponse> policyRevisions(int page, int size) {
        return queryService.policyRevisions(page, size);
    }

    @Transactional(readOnly = true)
    public LessonLogPolicyResponse getPolicy(LocalDate d) {
        return queryService.getPolicy(d);
    }

    @Transactional
    public LessonLogPolicyResponse putPolicy(ReqPolicyDTO r) {
        return policyService.putPolicy(r);
    }

    @Transactional
    public WeeklyReviewResponse signWeek(Long classId, ReqWeeklyReviewDTO r) {
        return weeklyCommandService.signWeek(classId, r, this::map, this::roster, this::rubric);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    private Long actor() {
        return AuditContext.currentUserId();
    }

    private AppException err(HttpStatus s, String m) {
        return new AppException(s, m);
    }

    // default access modifier retained for legacy scope-date characterization tests
    LocalDate homeroomScopeDate(LocalDate weekStart, Semester semester) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (semester.getEndDate() != null && weekEnd.isAfter(semester.getEndDate())) {
            return semester.getEndDate();
        }
        if (semester.getStartDate() != null && weekEnd.isBefore(semester.getStartDate())) {
            return semester.getStartDate();
        }
        return weekEnd;
    }

    private void writable(Semester s, LocalDateTime e, LocalDateTime x) {
        assertOpenSemester(s);
        writable(e, x);
    }

    private void assertOpenSemester(Semester s) {
        if (s.getStatus() == SemesterStatus.CLOSED || s.getStatus() == SemesterStatus.LOCKED) {
            throw err(HttpStatus.UNPROCESSABLE_ENTITY, "Học kỳ đã đóng");
        }
    }

    private void writable(LocalDateTime e, LocalDateTime x) {
        if (e.isAfter(now()) || !now().isBefore(x)) {
            throw err(HttpStatus.UNPROCESSABLE_ENTITY, "Tiết chưa kết thúc hoặc đã hết hạn chỉnh sửa");
        }
    }


    private String snapshot(LessonLogSource s, LessonLogPolicy p, LocalDate date, int rosterCount) {
        return "{\"timetableEntryId\":" + s.timetable().getId() + ",\"revisionId\":" + s.revision().getId()
                + ",\"revisionStatus\":" + quote(s.revision().getStatus().name()) + ",\"assignmentId\":"
                + s.assignment().getId() + ",\"classId\":" + s.classId() + ",\"subjectId\":" + s.subjectId()
                + ",\"teacherId\":" + s.assignment().getTeacherId() + ",\"semesterId\":" + s.semester().getId()
                + ",\"className\":" + quote(className(s.classId())) + ",\"subjectName\":"
                + quote(subjectName(s.subjectId())) + ",\"teacherName\":" + quote(teacherName(s.assignment().getTeacherId()))
                + ",\"functionalRoomName\":" + quote(roomName(s.timetable().getFunctionalRoomId()))
                + ",\"lessonDate\":" + quote(date.toString()) + ",\"session\":" + quote(s.period().getSession().name())
                + ",\"periodIndex\":" + s.period().getPeriodIndex() + ",\"startTime\":"
                + quote(s.period().getStartTime().toString()) + ",\"endTime\":" + quote(s.period().getEndTime().toString())
                + ",\"policyId\":" + p.getId() + ",\"policyVersion\":" + p.getPolicyVersion() + ",\"deadlineMode\":"
                + quote(p.getDeadlineMode()) + ",\"editWindowHours\":"
                + (p.getEditWindowHours() == null ? "null" : p.getEditWindowHours()) + ",\"rosterCountSnapshot\":"
                + rosterCount + "}";
    }

    private LessonLogEntryResponse map(LessonLogEntry e) {
        LessonLogEntryResponseMapper.DisplayValues displayValues = displayValues(e);
        boolean canEdit = canEdit(e);
        boolean canReview = canReview(e);
        boolean canAmend = canAmend(e);
        LessonLogEntryResponseMapper.ResponsePermissions permissions =
                new LessonLogEntryResponseMapper.ResponsePermissions(canEdit, canEdit && entryEditor.hasRequiredFields(e),
                        canReview, canAmend, canEdit || canReview || canAmend ? null
                                : LessonLogEntryAuthorization.blockedReason(e, teachers), rubricFor(e));
        return LessonLogEntryResponseMapper.map(e, displayValues, permissions);
    }

    private boolean canEdit(LessonLogEntry entry) {
        return LessonLogEntryAuthorization.canEdit(entry, teachers);
    }

    private boolean canReview(LessonLogEntry entry) {
        return LessonLogEntryAuthorization.canReview(entry);
    }

    private boolean canAmend(LessonLogEntry entry) {
        return LessonLogEntryAuthorization.canAmend(entry);
    }

    private List<Object> rubricFor(LessonLogEntry entry) {
        LessonLogPolicy policy = entry.getPolicyId() == null ? null : optional(policyService.findById(entry.getPolicyId()));
        return rubric(policy);
    }

    private String quote(String value) {
        return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private List<Object> rubric(LessonLogPolicy policy) {
        if (policy == null || policy.getRubricJson() == null || policy.getRubricJson().isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = (objectMapper == null ? new ObjectMapper() : objectMapper).readTree(policy.getRubricJson());
            if (!node.isArray()) {
                return List.of();
            }
            List<Object> values = new ArrayList<>();
            node.forEach(values::add);
            return values;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private <T> T optional(Optional<T> value) { return value == null ? null : value.orElse(null); }

    private String className(Long id) {
        SchoolClass value = schoolClasses == null ? null : optional(schoolClasses.findById(id));
        return value == null ? null : value.getClassName();
    }
    private String subjectName(Long id) {
        Subject value = subjects == null ? null : optional(subjects.findById(id));
        return value == null ? null : value.getName();
    }
    private String teacherName(Long id) {
        Teacher value = teachers == null ? null : optional(teachers.findById(id));
        return value == null ? null : value.getTeacherName();
    }
    private String roomName(Long id) {
        return scheduleService.roomName(id);
    }

    private LessonLogEntryResponseMapper.DisplayValues displayValues(LessonLogEntry e) {
        try {
            JsonNode node = (objectMapper == null ? new ObjectMapper() : objectMapper).readTree(e.getSourceSnapshotJson());
            return new LessonLogEntryResponseMapper.DisplayValues(text(node, "className", className(e.getClassId())), text(node, "subjectName", subjectName(e.getSubjectId())),
                    text(node, "teacherName", teacherName(e.getAssignedTeacherId())), text(node, "functionalRoomName", null));
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            return new LessonLogEntryResponseMapper.DisplayValues(className(e.getClassId()), subjectName(e.getSubjectId()), teacherName(e.getAssignedTeacherId()), null);
        }
    }
    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }
    private int roster(Long classId, LocalDateTime at) {
        return Math.toIntExact(enrollmentRepository.countRosterAt(classId, at));
    }

}
