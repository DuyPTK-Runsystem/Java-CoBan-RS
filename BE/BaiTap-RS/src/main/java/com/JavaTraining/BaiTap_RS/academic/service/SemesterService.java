package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqReopenSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessDecisionDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SemesterService {

    private static final Set<Integer> NOTIFICATION_OFFSETS = Set.of(
            -20, -10, -5, -3, -2, -1, 0, 1, 3, 5, 7, 14);

    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterMapper semesterMapper;
    private final AcademicCatalogAuditService auditService;

    public SemesterService(
            SemesterRepository semesterRepository,
            AcademicYearRepository academicYearRepository,
            SemesterMapper semesterMapper,
            AcademicCatalogAuditService auditService) {
        this.semesterRepository = semesterRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterMapper = semesterMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ResSemesterDTO> listByAcademicYear(Long academicYearId) {
        return semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId)
                .stream()
                .map(semesterMapper::toResponse)
                .toList();
    }

    @Transactional
    public ResSemesterDTO createSemester(ReqCreateSemesterDTO request) {
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
        Semester semester = findSemester(id);
        if (semester.getStatus() != SemesterStatus.DRAFT) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ học kỳ DRAFT mới được kích hoạt");
        }
        semester.setStatus(SemesterStatus.ACTIVE);
        return semesterMapper.toResponse(semester);
    }

    @Transactional
    public ResSemesterDTO lockSemester(Long id) {
        Semester semester = findSemester(id);
        if (semester.getStatus() != SemesterStatus.ACTIVE) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ học kỳ ACTIVE mới được khóa");
        }
        Map<String, Object> beforeData = semesterMapper.toAuditData(semester);
        semester.setStatus(SemesterStatus.LOCKED);
        semester.setLockedAt(LocalDateTime.now());
        semester.setLockedBy(AuditContext.currentUserId());
        auditService.writeAudit(
                "SEMESTER_LOCKED",
                "semester",
                semester.getId(),
                beforeData,
                semesterMapper.toAuditData(semester));
        return semesterMapper.toResponse(semester);
    }

    @Transactional
    public ResSemesterDTO reopenSemester(Long id, ReqReopenSemesterDTO request) {
        Semester semester = findSemester(id);
        if (semester.getStatus() != SemesterStatus.LOCKED) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ học kỳ LOCKED mới được mở lại");
        }
        Map<String, Object> beforeData = semesterMapper.toAuditData(semester);
        semester.setStatus(SemesterStatus.ACTIVE);
        semester.setLockReason(request.reason());
        auditService.writeAudit(
                "SEMESTER_REOPENED",
                "semester",
                semester.getId(),
                beforeData,
                semesterMapper.toAuditData(semester));
        return semesterMapper.toResponse(semester);
    }

    @Transactional(readOnly = true)
    public ResSemesterCompletenessDecisionDTO evaluateCompletenessCheckpoint(
            Long semesterId,
            LocalDate checkpointDate) {
        Semester semester = findSemester(semesterId);
        if (semester.getAutomaticLockAt() == null) {
            return new ResSemesterCompletenessDecisionDTO(
                    semesterId,
                    checkpointDate,
                    "NO_AUTOMATIC_LOCK",
                    "NO_NOTIFICATION");
        }
        int offset = (int) java.time.temporal.ChronoUnit.DAYS.between(
                semester.getAutomaticLockAt().toLocalDate(),
                checkpointDate);
        String decision = NOTIFICATION_OFFSETS.contains(offset) ? "NEEDS_NOTIFICATION" : "NO_NOTIFICATION";
        String checkpointCode = offset > 0 ? "t+" + offset + "d" : "t" + (offset == 0 ? "" : offset + "d");
        return new ResSemesterCompletenessDecisionDTO(semesterId, checkpointDate, checkpointCode, decision);
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
