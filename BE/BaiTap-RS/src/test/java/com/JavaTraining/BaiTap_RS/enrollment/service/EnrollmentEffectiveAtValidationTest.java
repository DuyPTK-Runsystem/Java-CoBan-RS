package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.ClassTransferHistory;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.ClassTransferHistoryRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentEffectiveAtValidationTest {

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
    void transferRejectsFutureEffectiveAtBeforePersistingHistory() {
        Mockito.when(lookupService.findEnrollment(50L)).thenReturn(enrollment());

        Assertions.assertThrows(
                AppException.class,
                () -> enrollmentService.transferEnrollment(
                        50L,
                        new ReqTransferEnrollmentDTO(
                                21L,
                                LocalDateTime.now().plusMinutes(1),
                                "Chuyển lớp")));

    }

    @Test
    void transferRejectsEffectiveAtBeforeLatestHistory() {
        LocalDateTime latestEffectiveAt = LocalDateTime.of(2026, 8, 20, 8, 0);
        Mockito.when(lookupService.findEnrollment(50L)).thenReturn(enrollment());
        Mockito.when(historyRepository.findTopByEnrollmentIdOrderByEffectiveAtDesc(50L))
                .thenReturn(Optional.of(history(latestEffectiveAt)));

        Assertions.assertThrows(
                AppException.class,
                () -> enrollmentService.transferEnrollment(
                        50L,
                        new ReqTransferEnrollmentDTO(
                                21L,
                                latestEffectiveAt.minusMinutes(1),
                                "Chuyển lớp")));

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

    private ClassTransferHistory history(LocalDateTime effectiveAt) {
        return new ClassTransferHistory(50L, 20L, 21L, effectiveAt, "Lịch sử trước", null);
    }
}
