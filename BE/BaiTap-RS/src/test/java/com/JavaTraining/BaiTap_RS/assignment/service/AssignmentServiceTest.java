package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqCreateHomeroomAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqReplaceAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
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
class AssignmentServiceTest {

    @Mock
    private HomeroomAssignmentRepository homeroomRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private AssignmentAuditService auditService;

    private HomeroomAssignmentService homeroomAssignmentService;

    @BeforeEach
    void setUp() {
        HomeroomAssignmentGuard guard = new HomeroomAssignmentGuard(
                schoolClassRepository,
                academicYearRepository,
                teacherRepository,
                homeroomRepository);
        homeroomAssignmentService = new HomeroomAssignmentService(
                homeroomRepository,
                guard,
                auditService);
    }

    @Test
    void createHomeroomRejectsExistingActiveAssignment() {
        SchoolClass schoolClass = schoolClass();
        Teacher teacher = teacher(TeacherStatus.ACTIVE);
        Mockito.when(schoolClassRepository.findById(20L)).thenReturn(Optional.of(schoolClass));
        Mockito.when(teacherRepository.findById(30L)).thenReturn(Optional.of(teacher));
        Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(academicYear()));
        Mockito.when(homeroomRepository.existsByClassIdAndStatus(20L, AssignmentStatus.ACTIVE))
                .thenReturn(true);

        assertConflict(() -> homeroomAssignmentService.createHomeroomAssignment(
                20L,
                new ReqCreateHomeroomAssignmentDTO(
                        30L,
                        LocalDate.of(2026, 9, 1),
                        null)));
    }

    @Test
    void createHomeroomRejectsInactiveTeacher() {
        Teacher teacher = teacher(TeacherStatus.INACTIVE);
        Mockito.when(schoolClassRepository.findById(20L)).thenReturn(Optional.of(schoolClass()));
        Mockito.when(teacherRepository.findById(30L)).thenReturn(Optional.of(teacher));

        assertConflict(() -> homeroomAssignmentService.createHomeroomAssignment(
                20L,
                new ReqCreateHomeroomAssignmentDTO(
                        30L,
                        LocalDate.of(2026, 9, 1),
                        null)));
    }

    @Test
    void replaceHomeroomEndsCurrentAndCreatesReplacement() {
        HomeroomAssignment current = new HomeroomAssignment(
                20L,
                30L,
                LocalDate.of(2026, 8, 15),
                null,
                AssignmentStatus.ACTIVE,
                1L);
        ReflectionTestUtils.setField(current, "id", 50L);
        Teacher replacementTeacher = teacher(TeacherStatus.ACTIVE);
        ReflectionTestUtils.setField(replacementTeacher, "id", 31L);
        Mockito.when(homeroomRepository.findById(50L)).thenReturn(Optional.of(current));
        Mockito.when(schoolClassRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(schoolClass()));
        Mockito.when(teacherRepository.findById(31L)).thenReturn(Optional.of(replacementTeacher));
        Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(academicYear()));
        Mockito.when(homeroomRepository.existsOverlap(
                Mockito.eq(20L),
                Mockito.eq(50L),
                Mockito.any(),
                Mockito.any())).thenReturn(false);
        Mockito.when(homeroomRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

        homeroomAssignmentService.replaceHomeroomAssignment(
                50L,
                new ReqReplaceAssignmentDTO(31L, LocalDate.of(2026, 10, 1), null));

        Assertions.assertEquals(
                AssignmentStatus.ENDED + ":2026-09-30",
                current.getStatus() + ":" + current.getValidTo(),
                "replacement should close current assignment");
    }

    private void assertConflict(Runnable action) {
        AppException exception = Assertions.assertThrows(AppException.class, action::run, "expected conflict");
        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(), "business guard should return conflict");
    }

    private AcademicYear academicYear() {
        AcademicYear year = new AcademicYear(
                "2026-2027",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2027, 5, 31),
                AcademicYearStatus.ACTIVE,
                null);
        ReflectionTestUtils.setField(year, "id", 10L);
        return year;
    }

    private SchoolClass schoolClass() {
        SchoolClass schoolClass = new SchoolClass(
                10L,
                6L,
                "6A",
                "6A",
                40,
                SchoolClassStatus.ACTIVE);
        ReflectionTestUtils.setField(schoolClass, "id", 20L);
        return schoolClass;
    }

    private Teacher teacher(TeacherStatus status) {
        Teacher teacher = new Teacher(
                null,
                "T001",
                "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                status);
        ReflectionTestUtils.setField(teacher, "id", 30L);
        return teacher;
    }
}
