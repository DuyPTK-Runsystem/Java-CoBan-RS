package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogClassResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogClassWeekResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogPolicyResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogRevisionResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogScheduleResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogRevisionRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogWeeklyReviewRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/** Owns lesson-log read use cases and response projection details. */
@Service
@RequiredArgsConstructor
public class LessonLogQueryService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String SEMESTER_NOT_FOUND = "Không tìm thấy học kỳ";

    private final LessonLogEntryRepository entries;
    private final LessonLogRevisionRepository audits;
    private final LessonLogPolicyService policyService;
    private final LessonLogWeeklySigningPolicyService weeklySigningPolicy;
    private final LessonLogScheduleService scheduleService;
    private final LessonLogWeeklyReviewRepository weeks;
    private final SemesterRepository semesters;
    private final SchoolClassRepository schoolClasses;
    private final HomeroomAssignmentRepository homerooms;
    private final TeacherRepository teachers;
    private final SubjectRepository subjects;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final LessonLogEntryEditor entryEditor;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public LessonLogEntryResponse get(Long id) {
        LessonLogEntry entry = load(id);
        LessonLogScopeAuthorization.assertEntryReadScope(entry, teachers, homerooms);
        return map(entry);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevision> revisions(Long id, int page, int size) {
        LessonLogEntry entry = load(id);
        LessonLogScopeAuthorization.assertEntryReadScope(entry, teachers, homerooms);
        Page<LessonLogRevision> result = audits.findByEntryIdOrderByCreatedAtDesc(id,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(result.getNumber(), result.getSize(),
                result.getTotalPages(), result.getTotalElements()), result.getContent());
    }

    @Transactional(readOnly = true)
    public LessonLogScheduleResponse mySchedule(LocalDate date) {
        if (date == null) {
            throw error(HttpStatus.BAD_REQUEST, "date là bắt buộc");
        }
        Long userId = AuditContext.currentUserId();
        if (userId == null) {
            return new LessonLogScheduleResponse(date, ZONE.getId(), List.of());
        }
        Long teacherId = teachers.findByUserId(userId)
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Tài khoản chưa có giáo viên")).getId();
        return scheduleService.teacherSchedule(date, teacherId, this::map, this::roster);
    }

    @Transactional(readOnly = true)
    public List<LessonLogClassResponse> classes(Long semesterId) {
        semesters.findById(semesterId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, SEMESTER_NOT_FOUND));
        Set<Long> classIds = new LinkedHashSet<>();
        if (LessonLogActorAuthorization.isManager()) {
            schoolClasses.findAllBySemesterId(semesterId).stream().map(SchoolClass::getId).forEach(classIds::add);
        } else {
            Long userId = AuditContext.currentUserId();
            Long teacherId = teachers.findByUserId(userId == null ? -1L : userId).map(Teacher::getId)
                    .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Tài khoản chưa có giáo viên"));
            Set<Long> semesterClassIds = schoolClasses.findAllBySemesterId(semesterId).stream()
                    .map(SchoolClass::getId).collect(java.util.stream.Collectors.toSet());
            homerooms.findClassIdsByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE).stream()
                    .filter(semesterClassIds::contains).forEach(classIds::add);
        }
        return schoolClasses.findAllByIdInOrderByClassCodeAsc(classIds).stream()
                .map(value -> new LessonLogClassResponse(value.getId(), semesterId, value.getClassCode(), value.getClassName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonLogClassWeekResponse classWeek(Long classId, Long semesterId, LocalDate weekStart) {
        requireMonday(weekStart);
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, SEMESTER_NOT_FOUND));
        LessonLogScopeAuthorization.assertClassScope(classId, homeroomScopeDate(weekStart, semester), teachers, homerooms);
        List<LessonLogEntry> logged = entries.findByClassIdAndSemesterIdAndLessonDateBetweenOrderByLessonDateAscPeriodIndexAsc(
                classId, semesterId, weekStart, weekStart.plusDays(6));
        LessonLogWeeklyReview review = weeks.findByClassIdAndSemesterIdAndWeekStart(classId, semesterId, weekStart)
                .orElse(null);
        List<LessonLogEntryResponse> resolved = scheduleService.classWeekItems(classId, semester, weekStart,
                weekStart.plusDays(6), logged, this::map, this::roster, this::rubric);
        boolean hasUnlogged = resolved.stream().anyMatch(item -> item.status() == LessonLogStatus.UNLOGGED);
        WeeklyReviewResponse weekly = review == null ? null : weekly(review,
                !hasUnlogged && weeklySigningPolicy.canSign(review, logged, semester, weekStart),
                hasUnlogged ? "Tuần còn tiết chưa ghi" : weeklySigningPolicy.blockedReason(logged, semester, weekStart));
        return new LessonLogClassWeekResponse(classId, semesterId, weekStart, weekStart.plusDays(6),
                scheduleService.calendarDays(semester, weekStart), resolved, weekly);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevisionResponse> weeklyRevisions(Long classId, Long semesterId,
            LocalDate weekStart, int page, int size) {
        requireMonday(weekStart);
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, SEMESTER_NOT_FOUND));
        LessonLogScopeAuthorization.assertClassScope(classId, homeroomScopeDate(weekStart, semester), teachers, homerooms);
        LessonLogWeeklyReview review = weeks.findByClassIdAndSemesterIdAndWeekStart(classId, semesterId, weekStart)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Chưa có tổng kết tuần"));
        return auditPage(audits.findByWeeklyReviewIdOrderByCreatedAtDesc(review.getId(), paging(page, size)));
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevisionResponse> policyRevisions(int page, int size) {
        return auditPage(audits.findPolicyRevisions(paging(page, size)));
    }

    @Transactional(readOnly = true)
    public LessonLogPolicyResponse getPolicy(LocalDate date) {
        return policyService.getPolicy(date);
    }

    private LessonLogEntry load(Long id) {
        return entries.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy sổ đầu bài"));
    }

    private LessonLogEntryResponse map(LessonLogEntry entry) {
        boolean canEdit = LessonLogEntryAuthorization.canEdit(entry, teachers);
        boolean canReview = LessonLogEntryAuthorization.canReview(entry);
        boolean canAmend = LessonLogEntryAuthorization.canAmend(entry);
        LessonLogEntryResponseMapper.ResponsePermissions permissions = new LessonLogEntryResponseMapper.ResponsePermissions(
                canEdit, canEdit && entryEditor.hasRequiredFields(entry), canReview, canAmend,
                canEdit || canReview || canAmend ? null : LessonLogEntryAuthorization.blockedReason(entry, teachers),
                rubricFor(entry));
        return LessonLogEntryResponseMapper.map(entry, displayValues(entry), permissions);
    }

    private LessonLogEntryResponseMapper.DisplayValues displayValues(LessonLogEntry entry) {
        try {
            JsonNode node = mapper().readTree(entry.getSourceSnapshotJson());
            return new LessonLogEntryResponseMapper.DisplayValues(text(node, "className", className(entry.getClassId())),
                    text(node, "subjectName", subjectName(entry.getSubjectId())),
                    text(node, "teacherName", teacherName(entry.getAssignedTeacherId())),
                    text(node, "functionalRoomName", null));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return new LessonLogEntryResponseMapper.DisplayValues(className(entry.getClassId()), subjectName(entry.getSubjectId()),
                    teacherName(entry.getAssignedTeacherId()), null);
        }
    }

    private List<Object> rubricFor(LessonLogEntry entry) {
        LessonLogPolicy policy = entry.getPolicyId() == null ? null : policyService.findById(entry.getPolicyId()).orElse(null);
        return rubric(policy);
    }

    private List<Object> rubric(LessonLogPolicy policy) {
        if (policy == null || policy.getRubricJson() == null || policy.getRubricJson().isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = mapper().readTree(policy.getRubricJson());
            if (!node.isArray()) {
                return List.of();
            }
            List<Object> values = new ArrayList<>();
            node.forEach(values::add);
            return values;
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private WeeklyReviewResponse weekly(LessonLogWeeklyReview review, boolean canSign, String blockedReason) {
        return new WeeklyReviewResponse(review.getId(), review.getClassId(), review.getSemesterId(), review.getWeekStart(),
                review.getWeekStart().plusDays(6), review.getStatus(), review.getWeeklyComment(), review.getWeeklyGrade(),
                review.getVersion(), review.getSignedAt(), review.getSignedBy(), canSign, blockedReason,
                review.getSignedSnapshotJson());
    }

    private ResultPaginationDTO<LessonLogRevisionResponse> auditPage(Page<LessonLogRevision> page) {
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(), page.getTotalPages(),
                page.getTotalElements()), page.getContent().stream().map(value -> new LessonLogRevisionResponse(value.getId(),
                value.getEntryId(), value.getWeeklyReviewId(), value.getPolicyId(), value.getAction(), value.getActorId(),
                value.getReason(), value.getBeforeStateJson(), value.getAfterStateJson(), value.getCorrelationId(),
                value.getCreatedAt())).toList());
    }

    private PageRequest paging(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void requireMonday(LocalDate date) {
        if (date == null || !date.equals(date.with(DayOfWeek.MONDAY))) {
            throw error(HttpStatus.BAD_REQUEST, "weekStart phải là thứ Hai");
        }
    }

    private LocalDate homeroomScopeDate(LocalDate weekStart, Semester semester) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (semester.getEndDate() != null && weekEnd.isAfter(semester.getEndDate())) {
            return semester.getEndDate();
        }
        if (semester.getStartDate() != null && weekEnd.isBefore(semester.getStartDate())) {
            return semester.getStartDate();
        }
        return weekEnd;
    }

    private int roster(Long classId, LocalDateTime at) {
        return Math.toIntExact(enrollmentRepository.countRosterAt(classId, at));
    }

    private String className(Long id) {
        return optional(schoolClasses.findById(id)).map(SchoolClass::getClassName).orElse(null);
    }

    private String teacherName(Long id) {
        return optional(teachers.findById(id)).map(Teacher::getTeacherName).orElse(null);
    }

    private String subjectName(Long id) {
        return optional(subjects.findById(id)).map(Subject::getName).orElse(null);
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private ObjectMapper mapper() {
        return objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    private <T> Optional<T> optional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}
