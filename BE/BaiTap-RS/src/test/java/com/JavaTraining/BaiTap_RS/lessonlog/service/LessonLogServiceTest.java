package com.JavaTraining.BaiTap_RS.lessonlog.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Optional;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.*;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.*;
import com.JavaTraining.BaiTap_RS.timetable.repository.*;
import com.JavaTraining.BaiTap_RS.assignment.repository.*;
import com.JavaTraining.BaiTap_RS.academic.repository.*;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class LessonLogServiceTest {
    @Mock LessonLogEntryRepository entries; @Mock LessonLogRevisionRepository audits;
    @Mock LessonLogPolicyRepository policies; @Mock LessonLogWeeklyReviewRepository weeks;
    @Mock TimetableEntryRepository timetableEntries; @Mock TimetableRevisionRepository timetableRevisions;
    @Mock TimetablePeriodRepository periods; @Mock TimetableCalendarRepository calendars;
    @Mock TimetableClosedDateRepository closedDates; @Mock SubjectTeachingAssignmentRepository assignments;
    @Mock ClassSubjectRepository classSubjects; @Mock SemesterRepository semesters;
    @Mock HomeroomAssignmentRepository homerooms; @Mock TeacherRepository teachers;
    @Mock com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository enrollments;
    @InjectMocks LessonLogService service;

    @Test void submit_requiresCompleteCountsAndTransitions() {
        LessonLogEntry e = entry(); e.setTitle("Bài 1"); e.setCompletionStatus("ON_SCHEDULE"); e.setGrade("A"); e.setRosterCountSnapshot(30); e.setPresentCount(29); e.setAbsentCount(1);
        when(entries.findById(7L)).thenReturn(Optional.of(e)); when(audits.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.submit(7L, 0L);
        assertEquals(LessonLogStatus.SUBMITTED, result.status()); verify(audits).save(any());
    }

    @Test void staleVersionReturnsConflictBeforeMutation() {
        LessonLogEntry e = entry(); when(entries.findById(7L)).thenReturn(Optional.of(e));
        AppException ex = assertThrows(AppException.class, () -> service.submit(7L, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus()); verifyNoInteractions(audits);
    }

    private LessonLogEntry entry() {
        LessonLogEntry e = new LessonLogEntry(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, LocalDate.now().minusDays(1), "MORNING", 1,
                "{}", LocalDateTime.now().minusHours(2), LocalDateTime.now().plusHours(2), null);
        org.springframework.test.util.ReflectionTestUtils.setField(e, "id", 7L);
        return e;
    }
}
