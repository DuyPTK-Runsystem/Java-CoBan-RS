package com.JavaTraining.BaiTap_RS.lessonlog.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Optional;
import java.util.List;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.*;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.*;
import com.JavaTraining.BaiTap_RS.timetable.repository.*;
import com.JavaTraining.BaiTap_RS.assignment.repository.*;
import com.JavaTraining.BaiTap_RS.academic.repository.*;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqTransitionLessonLogDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class LessonLogServiceTest {
    @Mock LessonLogEntryRepository entries; @Mock LessonLogRevisionRepository audits;
    @Mock LessonLogPolicyRepository policies; @Mock LessonLogWeeklyReviewRepository weeks;
    @Mock LessonLogPolicyService policyService;
    @Spy @InjectMocks LessonLogWeeklyReviewService weeklyReviewService;
    @Spy @InjectMocks LessonLogWeeklySnapshotService weeklySnapshotService;
    @Spy @InjectMocks LessonLogWeeklySigningPolicyService weeklySigningPolicy;
    @Spy @InjectMocks LessonLogWeeklyCommandSupport weeklyCommandSupport;
    @Spy @InjectMocks LessonLogEntryEditor entryEditor;
    @Spy @InjectMocks LessonLogAuditService auditService;
    @Spy @InjectMocks LessonLogEntryLifecycleService lifecycleService;
    @Spy @InjectMocks LessonLogEntryCommandService entryCommandService;
    @Mock TimetableEntryRepository timetableEntries; @Mock TimetableRevisionRepository timetableRevisions;
    @Mock TimetableHeadRepository timetableHeads;
    @Mock TimetableAuditRepository timetableAudits;
    @Mock TimetablePeriodRepository periods; @Mock TimetableCalendarRepository calendars;
    @Mock TimetableClosedDateRepository closedDates; @Mock SubjectTeachingAssignmentRepository assignments;
    @Mock ClassSubjectRepository classSubjects; @Mock SemesterRepository semesters;
    @Mock HomeroomAssignmentRepository homerooms; @Mock TeacherRepository teachers;
    @Mock com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository enrollments;
    private LessonLogSourceResolver sourceResolver;
    @InjectMocks LessonLogService service;

    @BeforeEach
    void defaultSemesterIsOpen() {
        TimetableSourceLookup timetableLookup = new TimetableSourceLookup(timetableEntries, timetableRevisions,
                timetableHeads, timetableAudits, periods);
        AcademicLessonSourceValidator academicValidator = new AcademicLessonSourceValidator(assignments, classSubjects,
                semesters, calendars, closedDates);
        sourceResolver = new LessonLogSourceResolver(timetableLookup, academicValidator);
        ReflectionTestUtils.setField(service, "sourceResolver", sourceResolver);
        ReflectionTestUtils.setField(service, "lifecycleService", lifecycleService);
        ReflectionTestUtils.setField(service, "entryCommandService", entryCommandService);
        ReflectionTestUtils.setField(lifecycleService, "entries", entries);
        ReflectionTestUtils.setField(lifecycleService, "editor", entryEditor);
        ReflectionTestUtils.setField(lifecycleService, "auditService", auditService);
        ReflectionTestUtils.setField(lifecycleService, "weeklyReviews", weeklyReviewService);
        ReflectionTestUtils.setField(entryCommandService, "entries", entries);
        ReflectionTestUtils.setField(entryCommandService, "semesters", semesters);
        ReflectionTestUtils.setField(entryCommandService, "teachers", teachers);
        ReflectionTestUtils.setField(entryCommandService, "lifecycle", lifecycleService);
        ReflectionTestUtils.setField(weeklyReviewService, "sourceResolver", sourceResolver);
        ReflectionTestUtils.setField(weeklyReviewService, "snapshotService", weeklySnapshotService);
        lenient().when(semesters.findById(4L)).thenReturn(Optional.of(semester(SemesterStatus.ACTIVE)));
        TimetableHead head = new TimetableHead(4L);
        ReflectionTestUtils.setField(head, "id", 1L);
        lenient().when(timetableHeads.findByIdAndSemesterIdForUpdate(anyLong(), anyLong()))
                .thenReturn(Optional.of(head));
        TimetableRevision revision = new TimetableRevision(1L, 4L, 1,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10), null);
        ReflectionTestUtils.setField(revision, "id", 2L);
        lenient().when(timetableRevisions.findById(2L)).thenReturn(Optional.of(revision));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test void submit_requiresCompleteCountsAndTransitions() throws Exception {
        LessonLogEntry e = entry(); e.setTitle("Bài 1"); e.setCompletionStatus("ON_SCHEDULE"); e.setGrade("A"); e.setRosterCountSnapshot(30); e.setPresentCount(29); e.setAbsentCount(1);
        when(entries.findById(7L)).thenReturn(Optional.of(e)); when(audits.save(any())).thenAnswer(i -> i.getArgument(0));
        e.setSourceSnapshotJson("{\"room\":\"A\\\"1\"}");
        var result = service.submit(7L, 0L);
        assertEquals(LessonLogStatus.SUBMITTED, result.status()); verify(audits).save(any());
        ArgumentCaptor<LessonLogRevision> revision = ArgumentCaptor.forClass(LessonLogRevision.class);
        verify(audits).save(revision.capture());
        assertNotNull(new ObjectMapper().readTree(revision.getValue().getBeforeStateJson()));
        assertNotNull(new ObjectMapper().readTree(revision.getValue().getAfterStateJson()));
        assertEquals("{\"room\":\"A\\\"1\"}",
                new ObjectMapper().readTree(revision.getValue().getBeforeStateJson()).get("sourceSnapshotJson").asText());
        assertEquals(7L, new ObjectMapper().readTree(revision.getValue().getAfterStateJson()).get("entryId").asLong());
    }

    @Test void staleVersionReturnsConflictBeforeMutation() {
        LessonLogEntry e = entry(); when(entries.findById(7L)).thenReturn(Optional.of(e));
        AppException ex = assertThrows(AppException.class, () -> service.submit(7L, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus()); verifyNoInteractions(audits);
    }

    @Test
    void weeklySnapshotContainsImmutableEntryAndHomeroomDataAsValidJson() throws Exception {
        LessonLogWeeklyReview review = new LessonLogWeeklyReview();
        ReflectionTestUtils.setField(review, "id", 91L);
        review.setClassId(5L);
        review.setSemesterId(4L);
        review.setWeekStart(LocalDate.of(2026, 9, 7));
        review.setWeeklyComment("Đã rà soát");
        review.setWeeklyGrade("A");
        review.setSignedBy(501L);
        review.setSignedAt(LocalDateTime.of(2026, 9, 14, 8, 0));
        HomeroomAssignment homeroom = new HomeroomAssignment(5L, 77L,
                LocalDate.of(2026, 8, 20), null, AssignmentStatus.ACTIVE, 900L);
        ReflectionTestUtils.setField(homeroom, "id", 31L);
        LessonLogEntry entry = entry();
        entry.setTitle("Tiết mở đầu");
        entry.setCompletionStatus("ON_SCHEDULE");
        entry.setGrade("A");
        entry.setPresentCount(29);
        entry.setAbsentCount(1);
        entry.setRosterCountSnapshot(30);
        entry.setSourceSnapshotJson("{\"room\":\"A\\\"1\"}");

        String snapshot = weeklyReviewService.signedSnapshot(review, List.of(entry), homeroom,
                auditService::snapshot);
        var json = new ObjectMapper().readTree(snapshot);

        assertEquals(31L, json.get("homeroomAssignment").get("assignmentId").asLong());
        assertEquals("{\"room\":\"A\\\"1\"}", json.get("entries").get(0)
                .get("sourceSnapshotJson").asText());
        assertEquals("ON_SCHEDULE", json.get("entries").get(0).get("completionStatus").asText());
        assertEquals(30, json.get("entries").get(0).get("rosterCountSnapshot").asInt());
    }

    @Test void submit_closedSemesterIsRejectedBeforeMutation() {
        when(semesters.findById(4L)).thenReturn(Optional.of(semester(SemesterStatus.CLOSED)));
        LessonLogEntry e = entry();
        when(entries.findById(7L)).thenReturn(Optional.of(e));

        AppException ex = assertThrows(AppException.class, () -> service.submit(7L, 0L));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verify(entries, never()).save(any());
        verifyNoInteractions(audits);
    }

    @Test
    void amend_appliesAllEditableFieldsBeforeCompletionAndAuditsBothStates() throws Exception {
        managerAuthentication();
        LessonLogEntry e = entry();
        e.setStatus(LessonLogStatus.SUBMITTED);
        e.setTitle("Cũ");
        e.setCompletionStatus("ON_SCHEDULE");
        e.setGrade("B");
        e.setPresentCount(8);
        e.setAbsentCount(2);
        e.setRosterCountSnapshot(10);
        when(entries.findById(7L)).thenReturn(Optional.of(e));
        when(entries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(audits.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.amend(7L, new ReqTransitionLessonLogDTO(0L, "Đã sửa", "Bổ sung chính xác",
                "Mới", "Nội dung mới", "AHEAD_OF_SCHEDULE", "A", 9, 1, "Nhận xét mới", "HS 10 vắng",
                "Ôn tập"));

        assertEquals(LessonLogStatus.AMENDED, result.status());
        assertEquals("Mới", e.getTitle());
        assertEquals("Nội dung mới", e.getContent());
        assertEquals("AHEAD_OF_SCHEDULE", e.getCompletionStatus());
        assertEquals("A", e.getGrade());
        assertEquals(9, e.getPresentCount());
        assertEquals(1, e.getAbsentCount());
        assertEquals("Nhận xét mới", e.getComments());
        verify(audits).save(argThat(a -> "AMEND".equals(a.getAction())
                && a.getAfterStateJson() != null && a.getBeforeStateJson() != null));
    }

    @Test
    void overdueDraftCanBeCompletedByAmendAndAuditedAsAmended() {
        managerAuthentication();
        LessonLogEntry e = entry();
        e.setStatus(LessonLogStatus.DRAFT);
        e.setEditWindowExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(1));
        e.setRosterCountSnapshot(10);
        when(entries.findById(7L)).thenReturn(Optional.of(e));
        when(entries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(audits.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.amend(7L, new ReqTransitionLessonLogDTO(0L, null, "Bổ sung quá hạn",
                "Tiết học", "Nội dung", "ON_SCHEDULE", "A", 10, 0, null, null, null));

        assertEquals(LessonLogStatus.AMENDED, result.status());
        verify(audits).save(argThat(audit -> "AMEND".equals(audit.getAction())
                && "Bổ sung quá hạn".equals(audit.getReason())));
    }

    @Test
    void draftBeforeDeadlineCannotBeAmended() {
        managerAuthentication();
        LessonLogEntry e = entry();
        e.setStatus(LessonLogStatus.DRAFT);
        when(entries.findById(7L)).thenReturn(Optional.of(e));

        AppException ex = assertThrows(AppException.class, () -> service.amend(7L,
                new ReqTransitionLessonLogDTO(0L, null, "Chưa đến hạn", "Tiết học", "Nội dung",
                        "ON_SCHEDULE", "A", 10, 0, null, null, null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verify(entries, never()).save(any());
        verifyNoInteractions(audits);
    }

    @Test
    void amendRequiresManagerEvenWhenVersionMatches() {
        AppException ex = assertThrows(AppException.class, () -> service.amend(7L,
                new ReqTransitionLessonLogDTO(0L, null, "Lý do", "Tiết học", "Nội dung",
                        "ON_SCHEDULE", "A", 10, 0, null, null, null)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verifyNoInteractions(audits);
    }

    @Test
    void weeklyResponseExposesCurrentExpectedEntryVersions() {
        LessonLogWeeklyReview review = new LessonLogWeeklyReview();
        ReflectionTestUtils.setField(review, "id", 91L);
        review.setClassId(5L);
        review.setSemesterId(4L);
        review.setWeekStart(LocalDate.of(2026, 9, 7));
        LessonLogEntry first = entry();
        first.setVersion(3L);

        var response = weeklyCommandSupport.response(review, false, "Tuần chưa kết thúc", List.of(first));

        assertEquals(List.of(new com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse.ExpectedEntry(7L, 3L)),
                response.expectedEntries());
    }

    @Test
    void weeklySigningRequiresCompletedWeekAndOpenSemester() {
        LessonLogWeeklyReview review = new LessonLogWeeklyReview();
        review.setStatus(WeeklyReviewStatus.UNSIGNED);
        LessonLogEntry completed = entry();
        completed.setStatus(LessonLogStatus.SUBMITTED);
        Semester open = semester(SemesterStatus.ACTIVE);
        LocalDate completedWeek = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate futureWeek = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .with(DayOfWeek.MONDAY);

        Boolean canSignCompleted = weeklySigningPolicy.canSign(review, List.of(completed),
                open, completedWeek);
        Boolean canSignFuture = weeklySigningPolicy.canSign(review, List.of(completed), open,
                futureWeek);
        assertTrue(canSignCompleted);
        assertFalse(canSignFuture);
        assertEquals("Học kỳ đã đóng", weeklySigningPolicy.blockedReason(List.of(completed),
                semester(SemesterStatus.CLOSED), completedWeek));
    }

    @Test
    void weeklyHomeroomScopeUsesWeekEndClampedToSemester() {
        Semester semester = new Semester(4L, "HK1", "Học kỳ 1", 1,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 12), null, SemesterStatus.ACTIVE);

        LocalDate scopeDate = ReflectionTestUtils.invokeMethod(service, "homeroomScopeDate",
                LocalDate.of(2026, 9, 7), semester);

        assertEquals(LocalDate.of(2026, 9, 12), scopeDate);
    }

    @Test
    void ensureWeeklyReviewLocksTimetableHeadBeforeReviewRow() {
        LessonLogEntry lesson = entry();
        when(weeks.findByClassIdAndSemesterIdAndWeekStartForUpdate(5L, 4L,
                lesson.getLessonDate().with(DayOfWeek.MONDAY))).thenReturn(Optional.empty());
        when(weeks.save(any(LessonLogWeeklyReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        weeklyReviewService.ensureExists(lesson);

        InOrder order = inOrder(timetableRevisions, timetableHeads, weeks);
        order.verify(timetableRevisions).findById(2L);
        order.verify(timetableHeads).findByIdAndSemesterIdForUpdate(1L, 4L);
        order.verify(weeks).findByClassIdAndSemesterIdAndWeekStartForUpdate(5L, 4L,
                lesson.getLessonDate().with(DayOfWeek.MONDAY));
    }

    @Test
    void invalidateWeekUsesLockedReviewRowAfterTimetableHeadLock() {
        LessonLogEntry lesson = entry();
        LessonLogWeeklyReview review = new LessonLogWeeklyReview();
        review.setClassId(5L);
        review.setSemesterId(4L);
        review.setWeekStart(lesson.getLessonDate().with(DayOfWeek.MONDAY));
        review.setStatus(WeeklyReviewStatus.SIGNED);
        when(weeks.findByClassIdAndSemesterIdAndWeekStartForUpdate(5L, 4L,
                review.getWeekStart())).thenReturn(Optional.of(review));
        when(weeks.save(any(LessonLogWeeklyReview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(audits.save(any(LessonLogRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        weeklyReviewService.invalidate(lesson);

        InOrder order = inOrder(timetableRevisions, timetableHeads, weeks);
        order.verify(timetableRevisions).findById(2L);
        order.verify(timetableHeads).findByIdAndSemesterIdForUpdate(1L, 4L);
        order.verify(weeks).findByClassIdAndSemesterIdAndWeekStartForUpdate(5L, 4L, review.getWeekStart());
        assertEquals(WeeklyReviewStatus.STALE, review.getStatus());
    }

    @Test void create_rejectsEndedAssignmentAsLessonSource() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        TimetableEntry timetable = new TimetableEntry(2L, 3L, 4L, null, date, date);
        TimetableRevision revision = new TimetableRevision(1L, 4L, 1, date, date, null);
        revision.setStatus(TimetableRevisionStatus.PUBLISHED);
        SubjectTeachingAssignment assignment = new SubjectTeachingAssignment(5L, 7L, date, date,
                AssignmentStatus.ENDED, 1L);
        when(timetableEntries.findById(1L)).thenReturn(Optional.of(timetable));
        when(timetableRevisions.findById(2L)).thenReturn(Optional.of(revision));
        when(assignments.findById(3L)).thenReturn(Optional.of(assignment));

        AppException ex = assertThrows(AppException.class, () -> service.create(
                new com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqCreateLessonLogDTO(
                        1L, date, null, null, null, null, null, null, null, null, null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verifyNoInteractions(entries);
        verify(timetableHeads).findByIdAndSemesterIdForUpdate(1L, 4L);
    }

    @Test void create_rejectsArchivedRevisionWithoutPublishAudit() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        TimetableEntry timetable = new TimetableEntry(2L, 3L, 4L, null, date, date);
        TimetableRevision revision = new TimetableRevision(1L, 4L, 1, date, date, null);
        revision.setStatus(TimetableRevisionStatus.ARCHIVED);
        ReflectionTestUtils.setField(revision, "id", 2L);
        when(timetableEntries.findById(1L)).thenReturn(Optional.of(timetable));
        when(timetableRevisions.findById(2L)).thenReturn(Optional.of(revision));
        when(timetableAudits.existsByRevisionIdAndAction(2L, "PUBLISH")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> service.create(
                new com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqCreateLessonLogDTO(
                        1L, date, null, null, null, null, null, null, null, null, null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verifyNoInteractions(entries);
        verify(timetableAudits).existsByRevisionIdAndAction(2L, "PUBLISH");
    }

    @Test void create_allowsArchivedRevisionWithPublishAudit() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        TimetableEntry timetable = new TimetableEntry(2L, 3L, 4L, null, date, date);
        TimetableRevision revision = new TimetableRevision(1L, 4L, 1, date, date, null);
        revision.setStatus(TimetableRevisionStatus.ARCHIVED);
        ReflectionTestUtils.setField(revision, "id", 2L);
        SubjectTeachingAssignment assignment = new SubjectTeachingAssignment(5L, 7L, date, date,
                AssignmentStatus.ENDED, 1L);
        when(timetableEntries.findById(1L)).thenReturn(Optional.of(timetable));
        when(timetableRevisions.findById(2L)).thenReturn(Optional.of(revision));
        when(timetableAudits.existsByRevisionIdAndAction(2L, "PUBLISH")).thenReturn(true);
        when(assignments.findById(3L)).thenReturn(Optional.of(assignment));

        AppException ex = assertThrows(AppException.class, () -> service.create(
                new com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqCreateLessonLogDTO(
                        1L, date, null, null, null, null, null, null, null, null, null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verify(timetableAudits).existsByRevisionIdAndAction(2L, "PUBLISH");
        verifyNoInteractions(entries);
    }

    private LessonLogEntry entry() {
        LessonLogEntry e = LessonLogEntry.create(new LessonLogEntrySourceData(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L,
                LocalDate.now().minusDays(1), "MORNING", 1, "{}", LocalDateTime.now().minusHours(2),
                LocalDateTime.now().plusHours(2), null));
        org.springframework.test.util.ReflectionTestUtils.setField(e, "id", 7L);
        return e;
    }

    private Semester semester(SemesterStatus status) {
        return new Semester(1L, "HK1", "Học kỳ 1", 1, LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(10), null, status);
    }

    private void managerAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("academic-office", "", "ROLE_ACADEMIC_OFFICE"));
    }
}
