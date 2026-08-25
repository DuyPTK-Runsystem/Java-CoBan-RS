package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.ClassTransferHistory;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.ClassTransferHistoryRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EnrollmentTransferServiceTest {

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
    void transferAppendsHistoryAndWritesBeforeAfterAudit() {
        AcademicYear year = year(10L);
        SchoolClass source = schoolClass(20L, 10L, 30L, "6A");
        SchoolClass target = schoolClass(21L, 10L, 30L, "6B");
        Student student = student(40L);
        StudentYearEnrollment enrollment = enrollment();
        Mockito.when(lookupService.findEnrollment(50L)).thenReturn(enrollment);
        Mockito.when(lookupService.findAcademicYear(10L)).thenReturn(year);
        Mockito.when(lookupService.findSchoolClass(20L)).thenReturn(source);
        Mockito.when(lookupService.findSchoolClass(21L)).thenReturn(target);
        Mockito.when(lookupService.findStudent(40L)).thenReturn(student);
        Mockito.when(historyRepository.save(Mockito.any(ClassTransferHistory.class)))
                .thenAnswer(invocation -> savedHistory(invocation.getArgument(0)));
        Mockito.when(capacityService.capacityWarnings(List.of(source, target))).thenReturn(List.of());

        enrollmentService.transferEnrollment(
                50L,
                new ReqTransferEnrollmentDTO(
                        21L,
                        LocalDateTime.now().minusMinutes(1),
                        "Cân bằng sĩ số"));

        requireTransferred(enrollment);
        checkTransferAudit(enrollment, source, target);
    }

    private StudentYearEnrollment enrollment() {
        StudentYearEnrollment enrollment = new StudentYearEnrollment(
                40L,
                10L,
                20L,
                EnrollmentStatus.ACTIVE,
                LocalDateTime.of(2026, 8, 1, 8, 0));
        enrollment.setId(50L);
        return enrollment;
    }

    private ClassTransferHistory savedHistory(ClassTransferHistory history) {
        history.setId(60L);
        return history;
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
        Student student = new Student("Nguyễn Văn A", "STU0000001");
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private void requireTransferred(StudentYearEnrollment enrollment) {
        Assertions.assertEquals(21L, enrollment.getCurrentClassId(), "transfer should update current class");
    }

    private void checkTransferAudit(
            StudentYearEnrollment enrollment,
            SchoolClass source,
            SchoolClass target) {
        Mockito.verify(auditService).writeTransferAudit(
                Mockito.isNull(),
                Mockito.same(enrollment),
                Mockito.same(source),
                Mockito.same(target),
                Mockito.any(ClassTransferHistory.class));
    }

}
