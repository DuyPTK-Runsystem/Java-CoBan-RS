package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.repository.ClassTransferHistoryRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
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
class AcademicServiceTest {

        @Mock
        private AcademicYearRepository academicYearRepository;

        @Mock
        private GradeLevelRepository gradeLevelRepository;

        @Mock
        private SchoolClassRepository schoolClassRepository;

        @Mock
        private StudentYearEnrollmentRepository enrollmentRepository;

        @Mock
        private ClassTransferHistoryRepository historyRepository;

        @Mock
        private TeacherRepository teacherRepository;

        @Mock
        private HomeroomAssignmentRepository homeroomAssignmentRepository;

        private AcademicYearService academicYearService;
        private SchoolClassService schoolClassService;

        @BeforeEach
        void setUp() {
                academicYearService = new AcademicYearService(
                                academicYearRepository,
                                schoolClassRepository,
                                enrollmentRepository,
                                new AcademicYearValidator(academicYearRepository));
                schoolClassService = new SchoolClassService(
                                academicYearRepository,
                                gradeLevelRepository,
                                schoolClassRepository,
                                new SchoolClassValidator(schoolClassRepository, enrollmentRepository,
                                                historyRepository),
                                teacherRepository,
                                homeroomAssignmentRepository);
        }

        @Test
        void createAcademicYearRejectsSecondActiveYear() {
                ReqCreateAcademicYearDTO request = new ReqCreateAcademicYearDTO(
                                "2026-2027",
                                LocalDate.of(2026, 8, 1),
                                LocalDate.of(2027, 5, 31),
                                AcademicYearStatus.ACTIVE,
                                null);
                Mockito.when(academicYearRepository.existsByCode(request.code())).thenReturn(false);
                Mockito.when(academicYearRepository.existsByStatus(AcademicYearStatus.ACTIVE)).thenReturn(true);

                AppException exception = Assertions.assertThrows(
                                AppException.class,
                                () -> academicYearService.createAcademicYear(request));

                requireConflictAndNoAcademicYearSave(exception);
        }

        @Test
        void deleteClassRejectsClassPresentOnlyInTransferHistory() {
                SchoolClass schoolClass = new SchoolClass(
                                10L,
                                30L,
                                "6A",
                                "6A",
                                40,
                                SchoolClassStatus.ACTIVE);
                ReflectionTestUtils.setField(schoolClass, "id", 20L);
                Mockito.when(schoolClassRepository.findById(20L)).thenReturn(Optional.of(schoolClass));
                Mockito.when(enrollmentRepository.existsByCurrentClassId(20L)).thenReturn(false);
                Mockito.when(historyRepository.existsByFromClassId(20L)).thenReturn(true);

                AppException exception = Assertions.assertThrows(
                                AppException.class,
                                () -> schoolClassService.deleteSchoolClass(20L));

                requireConflictAndNoSchoolClassDelete(exception);
        }

        @Test
        void updateClassRejectsGradeChangeWhenEnrollmentExists() {
                SchoolClass schoolClass = new SchoolClass(
                                10L,
                                30L,
                                "6A",
                                "6A",
                                40,
                                SchoolClassStatus.ACTIVE);
                ReflectionTestUtils.setField(schoolClass, "id", 20L);
                AcademicYear year = new AcademicYear(
                                "2026-2027",
                                LocalDate.of(2026, 8, 1),
                                LocalDate.of(2027, 5, 31),
                                AcademicYearStatus.ACTIVE,
                                null);
                ReflectionTestUtils.setField(year, "id", 10L);
                GradeLevel grade = new GradeLevel("GRADE_7", "Khối 7", 7, 2, null, true, null);
                ReflectionTestUtils.setField(grade, "id", 31L);
                Mockito.when(schoolClassRepository.findById(20L)).thenReturn(Optional.of(schoolClass));
                Mockito.when(academicYearRepository.findById(10L)).thenReturn(Optional.of(year));
                Mockito.when(gradeLevelRepository.findById(31L)).thenReturn(Optional.of(grade));
                Mockito.when(schoolClassRepository.existsByAcademicYearIdAndClassCodeAndIdNot(
                                10L, "6A", 20L)).thenReturn(false);
                Mockito.when(enrollmentRepository.existsByCurrentClassId(20L)).thenReturn(true);

                AppException exception = Assertions.assertThrows(
                                AppException.class,
                                () -> schoolClassService.updateSchoolClass(
                                                20L,
                                                new ReqUpdateSchoolClassDTO(31L, "6A", "6A", 40,
                                                                SchoolClassStatus.ACTIVE)));

                requireConflict(exception);
        }

        private void requireConflict(AppException exception) {
                Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(),
                                "business guard should return conflict");
        }

        private void requireConflictAndNoAcademicYearSave(AppException exception) {
                requireConflict(exception);
                Mockito.verify(academicYearRepository, Mockito.never()).save(Mockito.any());
        }

        private void requireConflictAndNoSchoolClassDelete(AppException exception) {
                requireConflict(exception);
                Mockito.verify(schoolClassRepository, Mockito.never()).delete(Mockito.any());
        }
}
