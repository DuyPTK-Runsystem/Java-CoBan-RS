package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqReopenSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.LockSource;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.service.CalculationTaskService;
import com.JavaTraining.BaiTap_RS.scorebook.service.TranscriptStateService;
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
@SuppressWarnings({
                "PMD.UnitTestContainsTooManyAsserts",
                "PMD.AvoidDuplicateLiterals"
})
class SemesterLockServiceTest {

        @Mock
        private SemesterRepository semesterRepository;

        @Mock
        private StudentYearEnrollmentRepository studentYearEnrollmentRepository;

        @Mock
        private TranscriptStateService transcriptStateService;

        @Mock
        private CalculationTaskService calculationTaskService;

        @Mock
        private AcademicCatalogAuditService auditService;

        private SemesterLockService lockService;

        @BeforeEach
        void setUp() {
                lockService = new SemesterLockService(
                                semesterRepository,
                                studentYearEnrollmentRepository,
                                transcriptStateService,
                                calculationTaskService,
                                new SemesterMapper(),
                                auditService);
        }

        @Test
        void manualLockSuccessMovesActiveToLockedAndTriggersRecalc() {
                Semester semester = semester(SemesterStatus.ACTIVE);
                Mockito.when(semesterRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(semester));
                Mockito.when(semesterRepository.save(Mockito.any(Semester.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                StudentYearEnrollment enrollment1 = enrollment(101L, 10L);
                StudentYearEnrollment enrollment2 = enrollment(102L, 10L);
                Mockito.when(studentYearEnrollmentRepository.findByAcademicYearIdAndStatusOrderByStudentIdAsc(
                                10L, EnrollmentStatus.ACTIVE))
                                .thenReturn(List.of(enrollment1, enrollment2));

                Mockito.when(transcriptStateService.touchTranscripts(101L, 10L, 1L)).thenReturn(2L);
                Mockito.when(transcriptStateService.touchTranscripts(102L, 10L, 1L)).thenReturn(3L);

                ResSemesterDTO result = lockService.lockSemester(1L, LockSource.MANUAL, 99L, "Lock thủ công", "REQ-1");

                Assertions.assertEquals(SemesterStatus.LOCKED, result.status(), "status must be LOCKED");
                Assertions.assertEquals(99L, result.lockedBy(), "lockedBy must match actorId");
                Assertions.assertNotNull(result.lockedAt(), "lockedAt must not be null");

                Mockito.verify(calculationTaskService).ensureRecalcTask(101L, 10L, 2L);
                Mockito.verify(calculationTaskService).ensureRecalcTask(102L, 10L, 3L);
                Mockito.verify(auditService).writeAudit(
                                Mockito.eq("SEMESTER_LOCKED"),
                                Mockito.eq("semester"),
                                Mockito.eq(1L),
                                Mockito.anyMap(),
                                Mockito.anyMap());
        }

        @Test
        void manualLockRejectsDraftSemester() {
                Semester semester = semester(SemesterStatus.DRAFT);
                Mockito.when(semesterRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(semester));

                AppException ex = Assertions.assertThrows(
                                AppException.class,
                                () -> lockService.lockSemester(1L, LockSource.MANUAL, 99L, null, null),
                                "locking draft semester should throw exception");
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus(), "status must be CONFLICT");
                Assertions.assertEquals("Chỉ học kỳ ACTIVE mới được khóa", ex.getMessage(),
                                "error message should match");
        }

        @Test
        void manualLockRejectsAlreadyLockedSemester() {
                Semester semester = semester(SemesterStatus.LOCKED);
                Mockito.when(semesterRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(semester));

                AppException ex = Assertions.assertThrows(
                                AppException.class,
                                () -> lockService.lockSemester(1L, LockSource.MANUAL, 99L, null, null),
                                "locking already locked semester manually should throw exception");
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus(), "status must be CONFLICT");
        }

        @Test
        void automaticLockIsIdempotentForAlreadyLockedSemester() {
                Semester semester = semester(SemesterStatus.LOCKED);
                Mockito.when(semesterRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(semester));

                ResSemesterDTO result = lockService.lockSemester(
                                1L, LockSource.AUTOMATIC, null, "Auto lock", "CORR-1");

                Assertions.assertEquals(SemesterStatus.LOCKED, result.status(), "status must be LOCKED");
                Mockito.verify(semesterRepository, Mockito.never()).save(Mockito.any());
                Mockito.verifyNoInteractions(transcriptStateService);
        }

        @Test
        void automaticLockRejectsDraftSemester() {
                Semester semester = semester(SemesterStatus.DRAFT);
                Mockito.when(semesterRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(semester));

                AppException ex = Assertions.assertThrows(
                                AppException.class,
                                () -> lockService.lockSemester(1L, LockSource.AUTOMATIC, null, null, null),
                                "auto lock on draft semester should throw exception");
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus(), "status must be CONFLICT");
        }

        @Test
        void reopenSemesterSuccessMovesLockedToActiveWithThreeDaysWindow() {
                Semester semester = semester(SemesterStatus.LOCKED);
                Mockito.when(semesterRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(semester));
                Mockito.when(semesterRepository.save(Mockito.any(Semester.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                StudentYearEnrollment enrollment = enrollment(101L, 10L);
                Mockito.when(studentYearEnrollmentRepository.findByAcademicYearIdAndStatusOrderByStudentIdAsc(
                                10L, EnrollmentStatus.ACTIVE))
                                .thenReturn(List.of(enrollment));
                Mockito.when(transcriptStateService.touchTranscripts(101L, 10L, 1L)).thenReturn(5L);

                ResSemesterDTO result = lockService.reopenSemester(
                                1L, new ReqReopenSemesterDTO("Mở lại để kiểm tra điểm"), 99L, "REQ-REOPEN-1");

                Assertions.assertEquals(SemesterStatus.ACTIVE, result.status(), "status must be ACTIVE");
                Assertions.assertNotNull(result.reopenUntil(), "reopenUntil must be set");
                Assertions.assertEquals("Mở lại để kiểm tra điểm", result.lockReason(), "reason must match");

                Mockito.verify(calculationTaskService).ensureRecalcTask(101L, 10L, 5L);
                Mockito.verify(auditService).writeAudit(
                                Mockito.eq("SEMESTER_REOPENED"),
                                Mockito.eq("semester"),
                                Mockito.eq(1L),
                                Mockito.anyMap(),
                                Mockito.anyMap());
        }

        @Test
        void reopenSemesterRejectsActiveSemester() {
                Semester semester = semester(SemesterStatus.ACTIVE);
                Mockito.when(semesterRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(semester));

                AppException ex = Assertions.assertThrows(
                                AppException.class,
                                () -> lockService.reopenSemester(1L, new ReqReopenSemesterDTO("Lý do"), 99L, null),
                                "reopening active semester should throw exception");
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus(), "status must be CONFLICT");
                Assertions.assertEquals("Chỉ học kỳ LOCKED mới được mở lại", ex.getMessage(), "message should match");
        }

        @Test
        void reopenSemesterRejectsBlankReason() {
                Semester semester = semester(SemesterStatus.LOCKED);
                Mockito.when(semesterRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(semester));

                AppException ex = Assertions.assertThrows(
                                AppException.class,
                                () -> lockService.reopenSemester(1L, new ReqReopenSemesterDTO("   "), 99L, null),
                                "reopening semester with blank reason should throw exception");
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(), "status must be BAD_REQUEST");
        }

        private Semester semester(SemesterStatus status) {
                Semester sem = new Semester(
                                10L,
                                "HK1",
                                "Học kỳ 1",
                                1,
                                LocalDate.of(2026, 8, 15),
                                LocalDate.of(2026, 12, 31),
                                LocalDateTime.of(2027, 2, 14, 0, 0),
                                status);
                ReflectionTestUtils.setField(sem, "id", 1L);
                return sem;
        }

        private StudentYearEnrollment enrollment(Long studentId, Long academicYearId) {
                StudentYearEnrollment enrollment = new StudentYearEnrollment(
                                studentId,
                                academicYearId,
                                50L,
                                EnrollmentStatus.ACTIVE,
                                LocalDateTime.now());
                ReflectionTestUtils.setField(enrollment, "id", studentId + 1000L);
                return enrollment;
        }
}
