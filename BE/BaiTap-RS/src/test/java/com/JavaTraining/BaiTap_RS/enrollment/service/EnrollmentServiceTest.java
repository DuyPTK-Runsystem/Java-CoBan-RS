package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqBulkCreateEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqCreateEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResEnrollmentMutationDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.ClassTransferHistory;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.ClassTransferHistoryRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.TooManyMethods",
        "PMD.UnitTestContainsTooManyAsserts",
        "PMD.AvoidDuplicateLiterals"
})
class EnrollmentServiceTest {

    @Mock
    private StudentYearEnrollmentRepository enrollmentRepository;

    @Mock
    private ClassTransferHistoryRepository historyRepository;

    @Mock
    private EnrollmentLookupService lookupService;

    @Mock
    private StudentLookupService studentLookupService;

    @Mock
    private EnrollmentCapacityService capacityService;

    @Mock
    private EnrollmentAuditService auditService;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(
                enrollmentRepository,
                historyRepository,
                lookupService,
                studentLookupService,
                capacityService,
                auditService);
    }

    @Test
    void createEnrollmentWritesInitialHistoryAndReturnsCreatedEnrollment() {
        AcademicYear year = year(10L);
        SchoolClass schoolClass = schoolClass(20L, 10L, 30L, "6A");
        Student student = student(40L);
        Mockito.when(lookupService.findAcademicYear(10L)).thenReturn(year);
        Mockito.when(lookupService.findSchoolClass(20L)).thenReturn(schoolClass);
        Mockito.when(studentLookupService.resolveStudent(40L, null)).thenReturn(student);
        Mockito.when(enrollmentRepository.existsByStudentIdAndAcademicYearId(40L, 10L)).thenReturn(false);
        Mockito.when(enrollmentRepository.save(Mockito.any(StudentYearEnrollment.class)))
                .thenAnswer(invocation -> {
                    StudentYearEnrollment enrollment = invocation.getArgument(0);
                    enrollment.setId(50L);
                    return enrollment;
                });
        Mockito.when(historyRepository.save(Mockito.any(ClassTransferHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(capacityService.capacityWarnings(List.of(schoolClass))).thenReturn(List.of());

        ResEnrollmentMutationDTO result = enrollmentService.createEnrollment(
                new ReqCreateEnrollmentDTO(40L, null, 10L, 20L, LocalDateTime.of(2026, 8, 21, 8, 0)));

        requireCreatedEnrollment(result);
        requireInitialHistory();
    }

    @Test
    void createEnrollmentRejectsDuplicateStudentYear() {
        AcademicYear year = year(10L);
        SchoolClass schoolClass = schoolClass(20L, 10L, 30L, "6A");
        Student student = student(40L);
        Mockito.when(lookupService.findAcademicYear(10L)).thenReturn(year);
        Mockito.when(lookupService.findSchoolClass(20L)).thenReturn(schoolClass);
        Mockito.when(studentLookupService.resolveStudent(40L, null)).thenReturn(student);
        Mockito.when(enrollmentRepository.existsByStudentIdAndAcademicYearId(40L, 10L)).thenReturn(true);

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> enrollmentService.createEnrollment(new ReqCreateEnrollmentDTO(40L, null, 10L, 20L, null)));

        requireConflictAndNoEnrollmentSave(exception);
    }

    @Test
    void createEnrollmentAcceptsStudentCode() {
        AcademicYear year = year(10L);
        SchoolClass schoolClass = schoolClass(20L, 10L, 30L, "6A");
        Student student = student(40L);
        Mockito.when(lookupService.findAcademicYear(10L)).thenReturn(year);
        Mockito.when(lookupService.findSchoolClass(20L)).thenReturn(schoolClass);
        Mockito.when(studentLookupService.resolveStudent(null, "STU0000001")).thenReturn(student);
        Mockito.when(enrollmentRepository.existsByStudentIdAndAcademicYearId(40L, 10L)).thenReturn(false);
        Mockito.when(enrollmentRepository.save(Mockito.any(StudentYearEnrollment.class)))
                .thenAnswer(invocation -> {
                    StudentYearEnrollment enrollment = invocation.getArgument(0);
                    enrollment.setId(50L);
                    return enrollment;
                });
        Mockito.when(historyRepository.save(Mockito.any(ClassTransferHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(capacityService.capacityWarnings(List.of(schoolClass))).thenReturn(List.of());

        ResEnrollmentMutationDTO result = enrollmentService.createEnrollment(
                new ReqCreateEnrollmentDTO(null, "STU0000001", 10L, 20L, null));

        Assertions.assertEquals("STU0000001", result.enrollments().get(0).studentCode(),
                "student code should be returned");
    }

    @Test
    void bulkEnrollmentRejectsDuplicateInputBeforeWriting() {
        AcademicYear year = year(10L);
        SchoolClass schoolClass = schoolClass(20L, 10L, 30L, "6A");
        Mockito.when(lookupService.findAcademicYear(10L)).thenReturn(year);
        Mockito.when(lookupService.findSchoolClass(20L)).thenReturn(schoolClass);

        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> enrollmentService.createBulkEnrollment(
                        new ReqBulkCreateEnrollmentDTO(10L, 20L, List.of(40L, 40L), null, null)));

        requireConflictAndNoEnrollmentSave(exception);
    }

    @Test
    void bulkEnrollmentAcceptsStudentCodes() {
        AcademicYear year = year(10L);
        SchoolClass schoolClass = schoolClass(20L, 10L, 30L, "6A");
        Student firstStudent = student(40L);
        Student secondStudent = student(41L, "STU0000002");
        Mockito.when(lookupService.findAcademicYear(10L)).thenReturn(year);
        Mockito.when(lookupService.findSchoolClass(20L)).thenReturn(schoolClass);
        Mockito.when(studentLookupService.resolveStudents(null, List.of("STU0000001", "STU0000002")))
                .thenReturn(List.of(firstStudent, secondStudent));
        Mockito.when(enrollmentRepository.save(Mockito.any(StudentYearEnrollment.class)))
                .thenAnswer(invocation -> {
                    StudentYearEnrollment enrollment = invocation.getArgument(0);
                    enrollment.setId(enrollment.getStudentId() + 10L);
                    return enrollment;
                });
        Mockito.when(historyRepository.save(Mockito.any(ClassTransferHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(capacityService.capacityWarnings(List.of(schoolClass))).thenReturn(List.of());

        ResEnrollmentMutationDTO result = enrollmentService.createBulkEnrollment(
                new ReqBulkCreateEnrollmentDTO(10L, 20L, null, List.of("STU0000001", "STU0000002"), null));

        Assertions.assertEquals(2, result.enrollments().size(), "two enrollments should be created");
        Assertions.assertEquals("STU0000002", result.enrollments().get(1).studentCode(),
                "second student code should be returned");
    }

    private AcademicYear year(Long id) {
        AcademicYear year = new AcademicYear(
                "2026-2027",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2027, 5, 31),
                AcademicYearStatus.ACTIVE,
                null);
        ReflectionTestUtils.setField(year, "id", id);
        return year;
    }

    private SchoolClass schoolClass(Long id, Long yearId, Long gradeId, String code) {
        SchoolClass schoolClass = new SchoolClass(
                yearId,
                gradeId,
                code,
                code,
                40,
                SchoolClassStatus.ACTIVE);
        ReflectionTestUtils.setField(schoolClass, "id", id);
        return schoolClass;
    }

    private Student student(Long id) {
        return student(id, "STU0000001");
    }

    private Student student(Long id, String code) {
        Student student = new Student("Nguyễn Văn A", "STU0000001");
        student.setStudentCode(code);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private void requireCreatedEnrollment(ResEnrollmentMutationDTO result) {
        Assertions.assertEquals(50L, result.enrollments().get(0).id(), "created enrollment id should be returned");
    }

    private void requireInitialHistory() {
        ArgumentCaptor<ClassTransferHistory> historyCaptor = ArgumentCaptor.forClass(ClassTransferHistory.class);
        Mockito.verify(historyRepository).save(historyCaptor.capture());
        ClassTransferHistory history = historyCaptor.getValue();
        boolean historyMatchesInitialEnrollment = Long.valueOf(50L).equals(history.getEnrollmentId())
                && history.getFromClassId() == null
                && Long.valueOf(20L).equals(history.getToClassId());
        Assertions.assertTrue(historyMatchesInitialEnrollment, "history should point to the initial target class");
    }

    private void requireConflict(AppException exception) {
        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatus(), "business guard should return conflict");
    }

    private void requireConflictAndNoEnrollmentSave(AppException exception) {
        requireConflict(exception);
        Mockito.verify(enrollmentRepository, Mockito.never()).save(Mockito.any());
    }
}
