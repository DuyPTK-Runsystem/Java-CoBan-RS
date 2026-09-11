package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqRejectUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailability;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailabilityStatus;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherUnavailabilityRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class TeacherUnavailabilityServiceTest {

        @Mock
        private TeacherUnavailabilityRepository unavailabilityRepository;

        @Mock
        private TeacherRepository teacherRepository;

        @Mock
        private SemesterRepository semesterRepository;

        @Mock
        private TimetableRevisionRepository revisionRepository;

        @Mock
        private TimetableEntryRepository entryRepository;

        @Mock
        private TimetablePeriodRepository periodRepository;

        @Mock
        private SubjectTeachingAssignmentRepository assignmentRepository;

        private TeacherUnavailabilityService service;

        @BeforeEach
        void setUp() {
                service = new TeacherUnavailabilityService(
                                unavailabilityRepository, teacherRepository, semesterRepository,
                                revisionRepository, entryRepository, periodRepository, assignmentRepository);
        }

        @Test
        void create_teacherRole_differentTeacher_throwsForbidden() {
                ReqCreateUnavailabilityDTO req = new ReqCreateUnavailabilityDTO(
                                1L, 100L, 2, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                                SessionType.MORNING, "1,2", "Bận việc gia đình");

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> service.create(req, 999L, true));
                Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }

        @Test
        void create_validToBeforeValidFrom_throwsBadRequest() {
                Mockito.when(teacherRepository.existsById(100L)).thenReturn(true);
                Mockito.when(semesterRepository.existsById(1L)).thenReturn(true);

                ReqCreateUnavailabilityDTO req = new ReqCreateUnavailabilityDTO(
                                1L, 100L, 2, null,
                                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 9, 1),
                                SessionType.MORNING, "1,2", "Bận việc gia đình");

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> service.create(req, 100L, true));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        void create_success() {
                Mockito.when(teacherRepository.existsById(100L)).thenReturn(true);
                Mockito.when(semesterRepository.existsById(1L)).thenReturn(true);

                ReqCreateUnavailabilityDTO req = new ReqCreateUnavailabilityDTO(
                                1L, 100L, 2, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                                SessionType.MORNING, "1,2", "Bận việc");

                TeacherUnavailability saved = new TeacherUnavailability(
                                100L, 1L, 2, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                                SessionType.MORNING, "1,2", "Bận việc");
                ReflectionTestUtils.setField(saved, "id", 1L);
                Mockito.when(unavailabilityRepository.save(Mockito.any())).thenReturn(saved);

                ResTeacherUnavailabilityDTO res = service.create(req, 100L, true);
                Assertions.assertEquals(100L, res.teacherId());
                Assertions.assertEquals(TeacherUnavailabilityStatus.PENDING, res.status());
        }

        @Test
        void approve_conflictWithPublishedTimetable_allowsApprovalWithWarning() {
                TeacherUnavailability unav = new TeacherUnavailability(
                                100L, 1L, 2, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                                SessionType.MORNING, "1,2", "Bận việc");
                ReflectionTestUtils.setField(unav, "id", 1L);
                ReflectionTestUtils.setField(unav, "version", 0L);
                Mockito.when(unavailabilityRepository.findById(1L)).thenReturn(Optional.of(unav));

                TimetableRevision rev = new TimetableRevision(1L, 1L, 1,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), null);
                ReflectionTestUtils.setField(rev, "id", 10L);
                Mockito.when(revisionRepository.findBySemesterIdAndStatus(1L, TimetableRevisionStatus.PUBLISHED))
                                .thenReturn(List.of(rev));

                TimetableEntry entry = new TimetableEntry(10L, 50L, 5L, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                Mockito.when(entryRepository.findByRevisionId(10L)).thenReturn(List.of(entry));

                SubjectTeachingAssignment assign = new SubjectTeachingAssignment(
                                1L, 100L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                Mockito.when(assignmentRepository.findById(50L)).thenReturn(Optional.of(assign));

                TimetablePeriod period = new TimetablePeriod(1L, 2, SessionType.MORNING, 1,
                                "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 45));
                Mockito.when(periodRepository.findById(5L)).thenReturn(Optional.of(period));
                Mockito.when(unavailabilityRepository.save(Mockito.any())).thenReturn(unav);

                ResTeacherUnavailabilityDTO res = service.approve(1L, 0L, 2L);
                Assertions.assertEquals(TeacherUnavailabilityStatus.APPROVED, res.status());
        }

        @Test
        void approve_noConflict_statusApproved() {
                TeacherUnavailability unav = new TeacherUnavailability(
                                100L, 1L, 2, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                                SessionType.MORNING, "1,2", "Bận việc");
                ReflectionTestUtils.setField(unav, "id", 1L);
                ReflectionTestUtils.setField(unav, "version", 0L);
                Mockito.when(unavailabilityRepository.findById(1L)).thenReturn(Optional.of(unav));

                // No published revisions
                Mockito.when(revisionRepository.findBySemesterIdAndStatus(1L, TimetableRevisionStatus.PUBLISHED))
                                .thenReturn(List.of());
                Mockito.when(unavailabilityRepository.save(Mockito.any())).thenReturn(unav);

                ResTeacherUnavailabilityDTO res = service.approve(1L, 0L, 2L);
                Assertions.assertEquals(TeacherUnavailabilityStatus.APPROVED, res.status());
        }

        @Test
        void reject_success() {
                TeacherUnavailability unav = new TeacherUnavailability(
                                100L, 1L, 2, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                                SessionType.MORNING, "1,2", "Bận việc");
                ReflectionTestUtils.setField(unav, "id", 1L);
                ReflectionTestUtils.setField(unav, "version", 0L);
                Mockito.when(unavailabilityRepository.findById(1L)).thenReturn(Optional.of(unav));
                Mockito.when(unavailabilityRepository.save(Mockito.any())).thenReturn(unav);

                ReqRejectUnavailabilityDTO req = new ReqRejectUnavailabilityDTO(0L, "Không đủ giáo viên thay thế");
                ResTeacherUnavailabilityDTO res = service.reject(1L, req, 2L);

                Assertions.assertEquals(TeacherUnavailabilityStatus.REJECTED, res.status());
                Assertions.assertEquals("Không đủ giáo viên thay thế", res.decisionReason());
        }

        @Test
        void withdraw_success() {
                TeacherUnavailability unav = new TeacherUnavailability(
                                100L, 1L, 2, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                                SessionType.MORNING, "1,2", "Bận việc");
                ReflectionTestUtils.setField(unav, "id", 1L);
                ReflectionTestUtils.setField(unav, "version", 0L);
                Mockito.when(unavailabilityRepository.findById(1L)).thenReturn(Optional.of(unav));
                Mockito.when(unavailabilityRepository.save(Mockito.any())).thenReturn(unav);

                ResTeacherUnavailabilityDTO res = service.withdraw(1L, 0L, 100L, true);
                Assertions.assertEquals(TeacherUnavailabilityStatus.WITHDRAWN, res.status());
        }
}
