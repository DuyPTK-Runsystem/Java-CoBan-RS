package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResAcademicYearStatisticsDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResCapacityWarningDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.service.EnrollmentCapacityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AcademicStatisticsServiceTest {

        @Mock
        private AcademicYearRepository academicYearRepository;

        @Mock
        private GradeLevelRepository gradeLevelRepository;

        @Mock
        private SchoolClassRepository schoolClassRepository;

        @Mock
        private StudentYearEnrollmentRepository enrollmentRepository;

        @Mock
        private EnrollmentCapacityService enrollmentCapacityService;

        @InjectMocks
        private AcademicStatisticsService statisticsService;

        @Test
        void getAcademicYearStatisticsThrowsWhenYearNotFound() {
                Mockito.when(academicYearRepository.findById(99L)).thenReturn(Optional.empty());

                AppException ex = Assertions.assertThrows(
                                AppException.class,
                                () -> statisticsService.getAcademicYearStatistics(99L));
                Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
                Assertions.assertEquals("Không tìm thấy năm học", ex.getMessage());
        }

        @Test
        void getAcademicYearStatisticsReturnsZeroStatsWhenNoClassesOrStudents() {
                AcademicYear year = new AcademicYear(
                                "2026-2027", LocalDate.now(), LocalDate.now().plusMonths(9),
                                AcademicYearStatus.ACTIVE, "Năm 2026");
                ReflectionTestUtils.setField(year, "id", 1L);

                GradeLevel grade6 = new GradeLevel("K6", "Khối 6", 6, 1, null, true, null);
                ReflectionTestUtils.setField(grade6, "id", 10L);

                Mockito.when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
                Mockito.when(schoolClassRepository.findAllByAcademicYearIdOrderByClassCodeAsc(1L))
                                .thenReturn(List.of());
                Mockito.when(enrollmentRepository.findByAcademicYearIdAndStatusOrderByStudentIdAsc(1L,
                                EnrollmentStatus.ACTIVE))
                                .thenReturn(List.of());
                Mockito.when(enrollmentCapacityService.capacityWarnings(List.of())).thenReturn(List.of());
                Mockito.when(gradeLevelRepository.findAll(Mockito.any(Sort.class))).thenReturn(List.of(grade6));

                ResAcademicYearStatisticsDTO result = statisticsService.getAcademicYearStatistics(1L);

                Assertions.assertNotNull(result);
                Assertions.assertEquals(1L, result.academicYearId());
                Assertions.assertEquals(0, result.totalWarnings());
                Assertions.assertTrue(result.classStatistics().isEmpty());
                Assertions.assertEquals(1, result.gradeStatistics().size());
                Assertions.assertEquals(0L, result.gradeStatistics().get(0).activeClassCount());
                Assertions.assertEquals(0L, result.gradeStatistics().get(0).activeStudentCount());
        }

        @Test
        void getAcademicYearStatisticsComputesStatsAndIncludesWarnings() {
                AcademicYear year = new AcademicYear(
                                "2026-2027", LocalDate.now(), LocalDate.now().plusMonths(9),
                                AcademicYearStatus.ACTIVE, "Năm 2026");
                ReflectionTestUtils.setField(year, "id", 1L);

                GradeLevel grade6 = new GradeLevel("K6", "Khối 6", 6, 1, null, true, null);
                ReflectionTestUtils.setField(grade6, "id", 10L);

                SchoolClass class6A = new SchoolClass(1L, 10L, "6A", "Lớp 6A", 35, SchoolClassStatus.ACTIVE);
                ReflectionTestUtils.setField(class6A, "id", 100L);

                SchoolClass class6B = new SchoolClass(1L, 10L, "6B", "Lớp 6B", 35, SchoolClassStatus.ACTIVE);
                ReflectionTestUtils.setField(class6B, "id", 101L);

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                StudentYearEnrollment e1 = new StudentYearEnrollment(1L, 1L, 100L, EnrollmentStatus.ACTIVE, now);
                StudentYearEnrollment e2 = new StudentYearEnrollment(2L, 1L, 100L, EnrollmentStatus.ACTIVE, now);
                StudentYearEnrollment e3 = new StudentYearEnrollment(3L, 1L, 101L, EnrollmentStatus.ACTIVE, now);

                ResCapacityWarningDTO warning6A = new ResCapacityWarningDTO(
                                100L, 1L, 10L, 2L, 1.5, "Sĩ số lớp lệch quá 20% so với trung bình khối");

                List<SchoolClass> classes = List.of(class6A, class6B);

                Mockito.when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
                Mockito.when(schoolClassRepository.findAllByAcademicYearIdOrderByClassCodeAsc(1L)).thenReturn(classes);
                Mockito.when(enrollmentRepository.findByAcademicYearIdAndStatusOrderByStudentIdAsc(1L,
                                EnrollmentStatus.ACTIVE))
                                .thenReturn(List.of(e1, e2, e3));
                Mockito.when(enrollmentCapacityService.capacityWarnings(classes)).thenReturn(List.of(warning6A));
                Mockito.when(gradeLevelRepository.findAll(Mockito.any(Sort.class))).thenReturn(List.of(grade6));

                ResAcademicYearStatisticsDTO result = statisticsService.getAcademicYearStatistics(1L);

                Assertions.assertNotNull(result);
                Assertions.assertEquals(1L, result.academicYearId());
                Assertions.assertEquals(1, result.totalWarnings());
                Assertions.assertEquals(2, result.classStatistics().size());

                // Grade stats check
                Assertions.assertEquals(1, result.gradeStatistics().size());
                Assertions.assertEquals(2L, result.gradeStatistics().get(0).activeClassCount());
                Assertions.assertEquals(3L, result.gradeStatistics().get(0).activeStudentCount());

                // Class stats check: 6A
                var stat6A = result.classStatistics().stream().filter(c -> c.classId().equals(100L)).findFirst()
                                .orElseThrow();
                Assertions.assertEquals("6A", stat6A.classCode());
                Assertions.assertEquals(2L, stat6A.activeStudentCount());
                Assertions.assertEquals(1.5, stat6A.gradeAverage());
                Assertions.assertNotNull(stat6A.warning());
                Assertions.assertEquals("Sĩ số lớp lệch quá 20% so với trung bình khối", stat6A.warning().message());

                // Class stats check: 6B
                var stat6B = result.classStatistics().stream().filter(c -> c.classId().equals(101L)).findFirst()
                                .orElseThrow();
                Assertions.assertEquals("6B", stat6B.classCode());
                Assertions.assertEquals(1L, stat6B.activeStudentCount());
                Assertions.assertNull(stat6B.warning());
        }
}
