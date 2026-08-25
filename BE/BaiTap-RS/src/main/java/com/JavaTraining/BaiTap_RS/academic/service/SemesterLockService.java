package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SemesterLockService {

    private final SemesterRepository semesterRepository;
    private final StudentYearEnrollmentRepository studentYearEnrollmentRepository;
    private final TranscriptStateService transcriptStateService;
    private final CalculationTaskService calculationTaskService;
    private final SemesterMapper semesterMapper;
    private final AcademicCatalogAuditService auditService;

    public SemesterLockService(
            SemesterRepository semesterRepository,
            StudentYearEnrollmentRepository studentYearEnrollmentRepository,
            TranscriptStateService transcriptStateService,
            CalculationTaskService calculationTaskService,
            SemesterMapper semesterMapper,
            AcademicCatalogAuditService auditService) {
        this.semesterRepository = semesterRepository;
        this.studentYearEnrollmentRepository = studentYearEnrollmentRepository;
        this.transcriptStateService = transcriptStateService;
        this.calculationTaskService = calculationTaskService;
        this.semesterMapper = semesterMapper;
        this.auditService = auditService;
    }

    @Transactional
    public ResSemesterDTO lockSemester(
            Long semesterId,
            LockSource source,
            Long actorId,
            String reason,
            String correlationId) {
        Semester semester = semesterRepository.findByIdForUpdate(semesterId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));

        if (semester.getStatus() != SemesterStatus.ACTIVE) {
            if (source == LockSource.MANUAL) {
                throw new AppException(HttpStatus.CONFLICT, "Chỉ học kỳ ACTIVE mới được khóa");
            }
            if (semester.getStatus() == SemesterStatus.LOCKED) {
                return semesterMapper.toResponse(semester);
            }
            throw new AppException(HttpStatus.CONFLICT, "Học kỳ không ở trạng thái ACTIVE để khóa tự động");
        }

        Map<String, Object> beforeData = semesterMapper.toAuditData(semester);

        semester.setStatus(SemesterStatus.LOCKED);
        semester.setLockedAt(LocalDateTime.now());
        semester.setLockedBy(actorId);
        semester.setLockReason(reason);
        Semester saved = semesterRepository.save(semester);

        triggerRecalculationForSemesterStudents(saved.getAcademicYearId(), saved.getId());

        Map<String, Object> afterData = semesterMapper.toAuditData(saved);
        afterData.put("lockSource", source.name());
        if (correlationId != null) {
            afterData.put("correlationId", correlationId);
        }

        auditService.writeAudit(
                "SEMESTER_LOCKED",
                "semester",
                saved.getId(),
                beforeData,
                afterData);

        return semesterMapper.toResponse(saved);
    }

    @Transactional
    public ResSemesterDTO reopenSemester(
            Long semesterId,
            ReqReopenSemesterDTO request,
            Long actorId,
            String correlationId) {
        Semester semester = semesterRepository.findByIdForUpdate(semesterId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));

        if (semester.getStatus() != SemesterStatus.LOCKED) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ học kỳ LOCKED mới được mở lại");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lý do mở lại học kỳ không được để trống");
        }

        Map<String, Object> beforeData = semesterMapper.toAuditData(semester);

        semester.setStatus(SemesterStatus.ACTIVE);
        semester.setReopenUntil(LocalDateTime.now().plusDays(3));
        semester.setLockReason(request.reason());
        Semester saved = semesterRepository.save(semester);

        triggerRecalculationForSemesterStudents(saved.getAcademicYearId(), saved.getId());

        Map<String, Object> afterData = semesterMapper.toAuditData(saved);
        if (correlationId != null) {
            afterData.put("correlationId", correlationId);
        }

        auditService.writeAudit(
                "SEMESTER_REOPENED",
                "semester",
                saved.getId(),
                beforeData,
                afterData);

        return semesterMapper.toResponse(saved);
    }

    private void triggerRecalculationForSemesterStudents(Long academicYearId, Long semesterId) {
        List<StudentYearEnrollment> activeEnrollments = studentYearEnrollmentRepository
                .findByAcademicYearIdAndStatusOrderByStudentIdAsc(academicYearId, EnrollmentStatus.ACTIVE);

        for (StudentYearEnrollment enrollment : activeEnrollments) {
            Long studentId = enrollment.getStudentId();
            long newSourceVersion = transcriptStateService.touchTranscripts(studentId, academicYearId, semesterId);
            calculationTaskService.ensureRecalcTask(studentId, academicYearId, newSourceVersion);
        }
    }
}
