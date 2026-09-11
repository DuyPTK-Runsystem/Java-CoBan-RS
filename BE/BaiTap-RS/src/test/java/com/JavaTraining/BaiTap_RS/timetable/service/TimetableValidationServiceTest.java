package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableReviewDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailability;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class TimetableValidationServiceTest {

        @Mock
        private TimetableRevisionRepository revisionRepository;

        @Mock
        private TimetableEntryRepository entryRepository;

        @Mock
        private TimetablePeriodRepository periodRepository;

        @Mock
        private SubjectTeachingAssignmentRepository assignmentRepository;

        @Mock
        private ClassSubjectRepository classSubjectRepository;

        @Mock
        private SchoolClassRepository schoolClassRepository;

        @Mock
        private SubjectRepository subjectRepository;

        @Mock
        private TeacherRepository teacherRepository;

        @Mock
        private FunctionalRoomRepository roomRepository;

        @Mock
        private SubjectFunctionalRoomRepository subjectFunctionalRoomRepository;

        @Mock
        private TeacherUnavailabilityRepository unavailabilityRepository;

        @Mock
        private TeacherLoadEvaluator teacherLoadEvaluator;

        private TimetableValidationService validationService;

        private TimetableRevision revision;

        @BeforeEach
        void setUp() {
                validationService = new TimetableValidationService(
                                revisionRepository, entryRepository, periodRepository,
                                assignmentRepository, classSubjectRepository, schoolClassRepository,
                                subjectRepository, teacherRepository, roomRepository,
                                subjectFunctionalRoomRepository, unavailabilityRepository,
                                teacherLoadEvaluator);

                revision = new TimetableRevision(1L, 1L, 1,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), null);
                ReflectionTestUtils.setField(revision, "id", 100L);
        }

        @Test
        void validateRevision_classOverlap_createsBlockingIssue() {
                Mockito.when(revisionRepository.findById(100L)).thenReturn(Optional.of(revision));

                TimetablePeriod p1 = new TimetablePeriod(1L, 2, SessionType.MORNING, 1, "Tiết 1",
                                LocalTime.of(7, 0), LocalTime.of(7, 45));
                ReflectionTestUtils.setField(p1, "id", 10L);

                TimetableEntry e1 = new TimetableEntry(100L, 1L, 10L, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e1, "id", 101L);

                TimetableEntry e2 = new TimetableEntry(100L, 2L, 10L, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e2, "id", 102L);

                Mockito.when(entryRepository.findByRevisionId(100L)).thenReturn(List.of(e1, e2));
                Mockito.when(periodRepository.findAllById(Mockito.anyList())).thenReturn(List.of(p1));

                SubjectTeachingAssignment a1 = new SubjectTeachingAssignment(
                                1L, 10L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a1, "id", 1L);
                SubjectTeachingAssignment a2 = new SubjectTeachingAssignment(
                                2L, 20L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a2, "id", 2L);
                Mockito.when(assignmentRepository.findAllById(Mockito.anyList())).thenReturn(List.of(a1, a2));

                ClassSubject cs1 = new ClassSubject(5L, 1L, 1L, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(cs1, "id", 1L);
                ClassSubject cs2 = new ClassSubject(5L, 2L, 1L, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(cs2, "id", 2L);
                Mockito.when(classSubjectRepository.findAllById(Mockito.anyList())).thenReturn(List.of(cs1, cs2));

                SchoolClass sc = new SchoolClass(1L, 1L, "10A1", "10A1", 40, SchoolClassStatus.ACTIVE);
                ReflectionTestUtils.setField(sc, "id", 5L);
                Mockito.when(schoolClassRepository.findAllById(Mockito.anyList())).thenReturn(List.of(sc));

                Teacher t1 = new Teacher(
                                1L, "GV01", "Thầy Nguyễn Văn X", LocalDate.of(1985, 1, 1), "MALE", "0900000001",
                                "gv01@school.edu.vn", "Toán", LocalDate.of(2015, 9, 1), TeacherStatus.ACTIVE);
                ReflectionTestUtils.setField(t1, "id", 10L);
                Teacher t2 = new Teacher(
                                2L, "GV02", "Thầy Nguyễn Văn Y", LocalDate.of(1985, 1, 1), "MALE", "0900000002",
                                "gv02@school.edu.vn", "Văn", LocalDate.of(2015, 9, 1), TeacherStatus.ACTIVE);
                ReflectionTestUtils.setField(t2, "id", 20L);
                Mockito.when(teacherRepository.findAllById(Mockito.anyList())).thenReturn(List.of(t1, t2));

                Mockito.when(unavailabilityRepository.findApprovedInSemester(Mockito.anyLong(), Mockito.any(),
                                Mockito.any()))
                                .thenReturn(List.of());

                Mockito.when(revisionRepository.save(Mockito.any())).thenReturn(revision);

                ResTimetableReviewDTO review = validationService.validateRevision(100L);
                Assertions.assertTrue(review.blockingCount() >= 1);
                Assertions.assertTrue(review.issues().stream()
                                .anyMatch(i -> "CLASS_OVERLAP".equals(i.code())));
        }

        @Test
        void validateRevision_teacherOverlap_createsBlockingIssue() {
                Mockito.when(revisionRepository.findById(100L)).thenReturn(Optional.of(revision));

                TimetablePeriod p1 = new TimetablePeriod(1L, 2, SessionType.MORNING, 1, "Tiết 1",
                                LocalTime.of(7, 0), LocalTime.of(7, 45));
                ReflectionTestUtils.setField(p1, "id", 10L);

                TimetableEntry e1 = new TimetableEntry(100L, 1L, 10L, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e1, "id", 101L);

                TimetableEntry e2 = new TimetableEntry(100L, 2L, 10L, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e2, "id", 102L);

                Mockito.when(entryRepository.findByRevisionId(100L)).thenReturn(List.of(e1, e2));
                Mockito.when(periodRepository.findAllById(Mockito.anyList())).thenReturn(List.of(p1));

                SubjectTeachingAssignment a1 = new SubjectTeachingAssignment(
                                1L, 10L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a1, "id", 1L);
                SubjectTeachingAssignment a2 = new SubjectTeachingAssignment(
                                2L, 10L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a2, "id", 2L);
                Mockito.when(assignmentRepository.findAllById(Mockito.anyList())).thenReturn(List.of(a1, a2));

                ClassSubject cs1 = new ClassSubject(5L, 1L, 1L, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(cs1, "id", 1L);
                ClassSubject cs2 = new ClassSubject(6L, 1L, 1L, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(cs2, "id", 2L);
                Mockito.when(classSubjectRepository.findAllById(Mockito.anyList())).thenReturn(List.of(cs1, cs2));

                Teacher t1 = new Teacher(
                                1L, "GV01", "Thầy Nguyễn Văn X", LocalDate.of(1985, 1, 1), "MALE", "0900000001",
                                "gv01@school.edu.vn", "Toán", LocalDate.of(2015, 9, 1), TeacherStatus.ACTIVE);
                ReflectionTestUtils.setField(t1, "id", 10L);
                Mockito.when(teacherRepository.findAllById(Mockito.anyList())).thenReturn(List.of(t1));

                Mockito.when(unavailabilityRepository.findApprovedInSemester(Mockito.anyLong(), Mockito.any(),
                                Mockito.any()))
                                .thenReturn(List.of());
                Mockito.when(revisionRepository.save(Mockito.any())).thenReturn(revision);

                ResTimetableReviewDTO review = validationService.validateRevision(100L);
                Assertions.assertTrue(review.issues().stream()
                                .anyMatch(i -> "TEACHER_OVERLAP".equals(i.code())));
        }

        @Test
        void validateRevision_roomOverlap_createsBlockingIssue() {
                Mockito.when(revisionRepository.findById(100L)).thenReturn(Optional.of(revision));

                TimetablePeriod p1 = new TimetablePeriod(1L, 2, SessionType.MORNING, 1, "Tiết 1",
                                LocalTime.of(7, 0), LocalTime.of(7, 45));
                ReflectionTestUtils.setField(p1, "id", 10L);

                TimetableEntry e1 = new TimetableEntry(100L, 1L, 10L, 99L,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e1, "id", 101L);

                TimetableEntry e2 = new TimetableEntry(100L, 2L, 10L, 99L,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e2, "id", 102L);

                Mockito.when(entryRepository.findByRevisionId(100L)).thenReturn(List.of(e1, e2));
                Mockito.when(periodRepository.findAllById(Mockito.anyList())).thenReturn(List.of(p1));

                SubjectTeachingAssignment a1 = new SubjectTeachingAssignment(
                                1L, 10L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a1, "id", 1L);
                SubjectTeachingAssignment a2 = new SubjectTeachingAssignment(
                                2L, 20L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a2, "id", 2L);
                Mockito.when(assignmentRepository.findAllById(Mockito.anyList())).thenReturn(List.of(a1, a2));

                ClassSubject cs1 = new ClassSubject(5L, 1L, 1L, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(cs1, "id", 1L);
                ClassSubject cs2 = new ClassSubject(6L, 2L, 1L, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(cs2, "id", 2L);
                Mockito.when(classSubjectRepository.findAllById(Mockito.anyList())).thenReturn(List.of(cs1, cs2));

                FunctionalRoom room = new FunctionalRoom("LAB_BIO", "Phòng Sinh học");
                ReflectionTestUtils.setField(room, "id", 99L);
                Mockito.when(roomRepository.findAllById(Mockito.anyList())).thenReturn(List.of(room));

                Mockito.when(subjectFunctionalRoomRepository
                                .existsBySubjectIdAndFunctionalRoomId(Mockito.anyLong(), Mockito.eq(99L)))
                                .thenReturn(true);

                Mockito.when(unavailabilityRepository.findApprovedInSemester(Mockito.anyLong(), Mockito.any(),
                                Mockito.any()))
                                .thenReturn(List.of());
                Mockito.when(revisionRepository.save(Mockito.any())).thenReturn(revision);

                ResTimetableReviewDTO review = validationService.validateRevision(100L);
                Assertions.assertTrue(review.issues().stream()
                                .anyMatch(i -> "ROOM_OVERLAP".equals(i.code())));
        }

        @Test
        void validateRevision_teacherUnavailable_createsBlockingIssue() {
                Mockito.when(revisionRepository.findById(100L)).thenReturn(Optional.of(revision));

                TimetablePeriod p1 = new TimetablePeriod(1L, 2, SessionType.MORNING, 1, "Tiết 1",
                                LocalTime.of(7, 0), LocalTime.of(7, 45));
                ReflectionTestUtils.setField(p1, "id", 10L);

                TimetableEntry e1 = new TimetableEntry(100L, 1L, 10L, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
                ReflectionTestUtils.setField(e1, "id", 101L);

                Mockito.when(entryRepository.findByRevisionId(100L)).thenReturn(List.of(e1));
                Mockito.when(periodRepository.findAllById(Mockito.anyList())).thenReturn(List.of(p1));

                SubjectTeachingAssignment a1 = new SubjectTeachingAssignment(
                                1L, 10L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                ReflectionTestUtils.setField(a1, "id", 1L);
                Mockito.when(assignmentRepository.findAllById(Mockito.anyList())).thenReturn(List.of(a1));

                ClassSubject cs1 = new ClassSubject(5L, 1L, 1L, ClassSubjectStatus.ACTIVE);
                ReflectionTestUtils.setField(cs1, "id", 1L);
                Mockito.when(classSubjectRepository.findAllById(Mockito.anyList())).thenReturn(List.of(cs1));

                Teacher t1 = new Teacher(
                                1L, "GV01", "Thầy Nguyễn Văn X", LocalDate.of(1985, 1, 1), "MALE", "0900000001",
                                "gv01@school.edu.vn", "Toán", LocalDate.of(2015, 9, 1), TeacherStatus.ACTIVE);
                ReflectionTestUtils.setField(t1, "id", 10L);
                Mockito.when(teacherRepository.findAllById(Mockito.anyList())).thenReturn(List.of(t1));

                TeacherUnavailability unav = new TeacherUnavailability(
                                10L, 1L, 2, null,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                                SessionType.MORNING, "1,2", "Lịch bận");
                Mockito.when(unavailabilityRepository.findApprovedInSemester(Mockito.anyLong(), Mockito.any(),
                                Mockito.any()))
                                .thenReturn(List.of(unav));
                Mockito.when(revisionRepository.save(Mockito.any())).thenReturn(revision);

                ResTimetableReviewDTO review = validationService.validateRevision(100L);
                Assertions.assertTrue(review.issues().stream()
                                .anyMatch(i -> "TEACHER_UNAVAILABLE".equals(i.code())));
        }

}

        