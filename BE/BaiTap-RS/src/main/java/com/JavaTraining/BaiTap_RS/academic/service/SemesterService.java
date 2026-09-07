package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqReopenSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessDecisionDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.LockSource;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GuardLogStatement"})
public class SemesterService {

    private static final Set<Integer> NOTIFICATION_OFFSETS = Set.of(
            -45, -30, -14, -7, -3, -1, 0, 1, 3, 7, 14);

    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterMapper semesterMapper;
    private final SemesterLockService semesterLockService;
    private final SemesterCompletenessService completenessService;

    public SemesterService(
            SemesterRepository semesterRepository,
            AcademicYearRepository academicYearRepository,
            SemesterMapper semesterMapper,
            SemesterLockService semesterLockService,
            SemesterCompletenessService completenessService) {
        this.semesterRepository = semesterRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterMapper = semesterMapper;
        this.semesterLockService = semesterLockService;
        this.completenessService = completenessService;
    }

    @Transactional(readOnly = true)
    public List<ResSemesterDTO> listByAcademicYear(Long academicYearId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterService.class,
                "SemesterService.listByAcademicYear");
        return semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId)
                .stream()
                .map(semesterMapper::toResponse)
                .toList();
    }

    @Transactional
    public ResSemesterDTO createSemester(ReqCreateSemesterDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterService.class,
                "SemesterService.createSemester");
        AcademicYear year = findAcademicYear(request.academicYearId());
        validateDatesWithinYear(year, request.startDate(), request.endDate());
        if (semesterRepository.existsByAcademicYearIdAndCode(request.academicYearId(), request.code())) {
            throw new AppException(HttpStatus.CONFLICT, "Mã học kỳ đã tồn tại trong năm học");
        }
        if (semesterRepository.existsByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                request.academicYearId(), request.endDate(), request.startDate())) {
            throw new AppException(HttpStatus.CONFLICT, "Thời gian học kỳ bị chồng lấn");
        }
        SemesterStatus status = request.status() != null ? request.status() : SemesterStatus.DRAFT;
        Semester semester = new Semester(
                request.academicYearId(),
                request.code(),
                request.name(),
                request.displayOrder(),
                request.startDate(),
                request.endDate(),
                request.automaticLockAt(),
                status);
        return semesterMapper.toResponse(semesterRepository.save(semester));
    }

    @Transactional
    public ResSemesterDTO updateSemester(Long id, ReqUpdateSemesterDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterService.class,
                "SemesterService.updateSemester");
        Semester semester = findSemester(id);
        if (semester.getStatus() == SemesterStatus.CLOSED) {
            throw new AppException(HttpStatus.CONFLICT, "Không thể cập nhật học kỳ đã CLOSED");
        }
        AcademicYear year = findAcademicYear(semester.getAcademicYearId());
        validateDatesWithinYear(year, request.startDate(), request.endDate());
        if (semesterRepository.existsByAcademicYearIdAndCodeAndIdNot(
                semester.getAcademicYearId(), request.code(), id)) {
            throw new AppException(HttpStatus.CONFLICT, "Mã học kỳ đã tồn tại trong năm học");
        }
        if (semesterRepository.existsByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                semester.getAcademicYearId(), request.endDate(), request.startDate(), id)) {
            throw new AppException(HttpStatus.CONFLICT, "Thời gian học kỳ bị chồng lấn");
        }
        semester.setCode(request.code());
        semester.setName(request.name());
        semester.setDisplayOrder(request.displayOrder());
        semester.setStartDate(request.startDate());
        semester.setEndDate(request.endDate());
        semester.setAutomaticLockAt(request.automaticLockAt());
        if (request.status() != null) {
            semester.setStatus(request.status());
        }
        return semesterMapper.toResponse(semester);
    }

    @Transactional
    public ResSemesterDTO activateSemester(Long id) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterService.class,
                "SemesterService.activateSemester");
        Semester semester = findSemester(id);
        if (semester.getStatus() != SemesterStatus.DRAFT) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ học kỳ DRAFT mới được kích hoạt");
        }
        semester.setStatus(SemesterStatus.ACTIVE);
        return semesterMapper.toResponse(semester);
    }

    @Transactional
    public ResSemesterDTO lockSemester(Long id) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterService.class,
                "SemesterService.lockSemester");
        return semesterLockService.lockSemester(
                id,
                LockSource.MANUAL,
                AuditContext.currentUserId(),
                null,
                AuditContext.requestId());
    }

    @Transactional
    public ResSemesterDTO reopenSemester(Long id, ReqReopenSemesterDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterService.class,
                "SemesterService.reopenSemester");
        return semesterLockService.reopenSemester(
                id,
                request,
                AuditContext.currentUserId(),
                AuditContext.requestId());
    }

    @Transactional(readOnly = true)
    public ResSemesterCompletenessDecisionDTO evaluateCompletenessCheckpoint(
            Long semesterId,
            LocalDate checkpointDate) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterService.class,
                        "SemesterService.evaluateCompletenessCheckpoint");
        Semester semester = findSemester(semesterId);
        LocalDate lockDate = calculateEffectiveLockDate(semester);

        int offset = (int) ChronoUnit.DAYS.between(lockDate, checkpointDate);
        String checkpointCode;
        if (offset == 0) {
            checkpointCode = "t";
        } else if (offset > 0) {
            checkpointCode = "t+" + offset + "d";
        } else {
            checkpointCode = "t" + offset + "d";
        }

        if (!NOTIFICATION_OFFSETS.contains(offset)) {
            return new ResSemesterCompletenessDecisionDTO(
                    semesterId,
                    checkpointDate,
                    checkpointCode,
                    "NO_NOTIFICATION");
        }

        SemesterCompletenessSummaryDTO summary = completenessService.evaluateCompleteness(semesterId);
        String decision = summary.complete() ? "NO_NOTIFICATION" : "NEEDS_NOTIFICATION";
        return new ResSemesterCompletenessDecisionDTO(semesterId, checkpointDate, checkpointCode, decision);
    }

    public LocalDate calculateEffectiveLockDate(Semester semester) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterService.class,
                "SemesterService.calculateEffectiveLockDate");
        if (semester.getReopenUntil() != null) {
            return semester.getReopenUntil().toLocalDate();
        }
        if (semester.getAutomaticLockAt() != null) {
            return semester.getAutomaticLockAt().toLocalDate();
        }
        return semester.getEndDate().plusDays(45);
    }

    private Semester findSemester(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học"));
    }

    private void validateDatesWithinYear(AcademicYear year, LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new AppException(HttpStatus.CONFLICT, "Ngày kết thúc học kỳ phải sau ngày bắt đầu");
        }
        if (startDate.isBefore(year.getStartDate()) || endDate.isAfter(year.getEndDate())) {
            throw new AppException(HttpStatus.CONFLICT, "Học kỳ phải nằm trong thời gian năm học");
        }
    }
}
