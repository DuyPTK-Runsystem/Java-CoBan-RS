package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqRejectUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqUpdateUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailability;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailabilityStatus;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherUnavailabilityRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings({ "PMD.ExcessiveImports", "PMD.CouplingBetweenObjects", "PMD.GodClass", "PMD.TooManyMethods" })
public class TeacherUnavailabilityService {

    private static final String CONFLICT_CONCURRENT_MSG = "Đơn đã bị sửa đổi bởi người khác. Vui lòng tải lại.";

    private final TeacherUnavailabilityRepository unavailabilityRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final TimetableRevisionRepository revisionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetablePeriodRepository periodRepository;
    private final SubjectTeachingAssignmentRepository assignmentRepository;

    @Transactional
    public ResTeacherUnavailabilityDTO create(ReqCreateUnavailabilityDTO req, Long currentTeacherId,
            boolean isTeacherRole) {
        Long targetTeacherId = req.teacherId() != null ? req.teacherId() : currentTeacherId;
        validateCreateAccess(targetTeacherId, currentTeacherId, isTeacherRole);

        if (!teacherRepository.existsById(targetTeacherId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giáo viên");
        }
        if (!semesterRepository.existsById(req.semesterId())) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ");
        }
        validateDateRange(req.validFrom(), req.validTo());

        TeacherUnavailability record = new TeacherUnavailability(
                targetTeacherId,
                req.semesterId(),
                req.dayOfWeek(),
                req.specificDate(),
                req.validFrom(),
                req.validTo(),
                req.session(),
                req.periodIndexes(),
                req.note());
        record = unavailabilityRepository.save(record);
        return toDTO(record);
    }

    @Transactional
    public ResTeacherUnavailabilityDTO update(Long id, ReqUpdateUnavailabilityDTO req,
            Long currentTeacherId, boolean isTeacherRole) {
        TeacherUnavailability record = findUnavailability(id);
        validateOwnerAccess(record.getTeacherId(), currentTeacherId, isTeacherRole, "sửa");
        validateExpectedVersion(record.getVersion(), req.expectedVersion());
        validatePendingStatus(record.getStatus(), "sửa");
        validateDateRange(req.validFrom(), req.validTo());

        applyUpdateFields(record, req);
        record = unavailabilityRepository.save(record);
        return toDTO(record);
    }

    @Transactional
    public ResTeacherUnavailabilityDTO withdraw(Long id, Long expectedVersion,
            Long currentTeacherId, boolean isTeacherRole) {
        TeacherUnavailability record = findUnavailability(id);
        validateOwnerAccess(record.getTeacherId(), currentTeacherId, isTeacherRole, "rút");
        validateExpectedVersion(record.getVersion(), expectedVersion);

        if (record.getStatus() == TeacherUnavailabilityStatus.WITHDRAWN) {
            return toDTO(record);
        }
        record.setStatus(TeacherUnavailabilityStatus.WITHDRAWN);
        record = unavailabilityRepository.save(record);
        return toDTO(record);
    }

    @Transactional
    public ResTeacherUnavailabilityDTO approve(Long id, Long expectedVersion, Long currentUserId) {
        TeacherUnavailability record = findUnavailability(id);
        validateExpectedVersion(record.getVersion(), expectedVersion);
        validatePendingStatus(record.getStatus(), "duyệt");

        checkConflictWithPublishedTimetables(record);

        record.setStatus(TeacherUnavailabilityStatus.APPROVED);
        record.setDecidedBy(currentUserId);
        record.setDecidedAt(LocalDateTime.now());
        record.setDecisionReason(null);
        record = unavailabilityRepository.save(record);
        return toDTO(record);
    }

    @Transactional
    public ResTeacherUnavailabilityDTO reject(Long id, ReqRejectUnavailabilityDTO req, Long currentUserId) {
        TeacherUnavailability record = findUnavailability(id);
        validateExpectedVersion(record.getVersion(), req.expectedVersion());
        validatePendingStatus(record.getStatus(), "từ chối");

        record.setStatus(TeacherUnavailabilityStatus.REJECTED);
        record.setDecidedBy(currentUserId);
        record.setDecidedAt(LocalDateTime.now());
        record.setDecisionReason(req.reason());
        record = unavailabilityRepository.save(record);
        return toDTO(record);
    }

    private void validateExpectedVersion(Long currentVersion, Long expectedVersion) {
        if (expectedVersion != null && !Objects.equals(currentVersion, expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT, CONFLICT_CONCURRENT_MSG);
        }
    }

    private void validatePendingStatus(TeacherUnavailabilityStatus status, String action) {
        if (status != TeacherUnavailabilityStatus.PENDING) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Chỉ có thể " + action + " đơn khi đang ở trạng thái Chờ duyệt");
        }
    }

    private void validateCreateAccess(Long targetTeacherId, Long currentTeacherId, boolean isTeacherRole) {
        if (targetTeacherId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Không xác định được thông tin giáo viên");
        }
        if (isTeacherRole && currentTeacherId != null && !Objects.equals(targetTeacherId, currentTeacherId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Giáo viên chỉ được đăng ký lịch bận cho chính mình");
        }
    }

    private void validateOwnerAccess(Long recordTeacherId, Long currentTeacherId,
            boolean isTeacherRole, String action) {
        if (isTeacherRole && currentTeacherId != null && !Objects.equals(recordTeacherId, currentTeacherId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền " + action + " đơn của giáo viên khác");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "validTo phải sau hoặc bằng validFrom");
        }
    }

    private void applyUpdateFields(TeacherUnavailability record, ReqUpdateUnavailabilityDTO req) {
        record.setDayOfWeek(req.dayOfWeek());
        record.setSpecificDate(req.specificDate());
        record.setValidFrom(req.validFrom());
        record.setValidTo(req.validTo());
        record.setSession(req.session());
        record.setPeriodIndexes(req.periodIndexes());
        record.setNote(req.note());
    }

    private void checkConflictWithPublishedTimetables(TeacherUnavailability record) {
        List<TimetableRevision> publishedRevisions = revisionRepository
                .findBySemesterIdAndStatus(record.getSemesterId(), TimetableRevisionStatus.PUBLISHED);
        Set<Integer> targetPeriodIndexes = parsePeriodIndexes(record.getPeriodIndexes());

        for (TimetableRevision revision : publishedRevisions) {
            checkRevisionConflict(revision, record, targetPeriodIndexes);
        }
    }

    private void checkRevisionConflict(
            TimetableRevision revision,
            TeacherUnavailability record,
            Set<Integer> targetPeriodIndexes) {
        List<TimetableEntry> entries = entryRepository.findByRevisionId(revision.getId());
        for (TimetableEntry entry : entries) {
            if (isEntryConflictWithUnavailability(entry, record, targetPeriodIndexes)) {
                throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Đăng ký lịch bận trùng với tiết học của thời khóa biểu đã công bố. "
                                + "Vui lòng điều chỉnh thời khóa biểu trước khi duyệt.");
            }
        }
    }

    private boolean isEntryConflictWithUnavailability(
            TimetableEntry entry,
            TeacherUnavailability record,
            Set<Integer> targetPeriodIndexes) {
        if (entry.getValidFrom().isAfter(record.getValidTo())
                || entry.getValidTo().isBefore(record.getValidFrom())) {
            return false;
        }
        SubjectTeachingAssignment assignment = assignmentRepository.findById(entry.getAssignmentId()).orElse(null);
        if (assignment == null || !Objects.equals(assignment.getTeacherId(), record.getTeacherId())) {
            return false;
        }
        TimetablePeriod period = periodRepository.findById(entry.getPeriodId()).orElse(null);
        if (period == null) {
            return false;
        }
        if (record.getDayOfWeek() != null && !Objects.equals(period.getDayOfWeek(), record.getDayOfWeek())) {
            return false;
        }
        return period.getSession() == record.getSession()
                && targetPeriodIndexes.contains(period.getPeriodIndex());
    }

    @Transactional(readOnly = true)
    public List<ResTeacherUnavailabilityDTO> list(
            Long semesterId,
            Long teacherId,
            TeacherUnavailabilityStatus status,
            LocalDate from,
            LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.of(2000, 1, 1);
        LocalDate effectiveTo = to != null ? to : LocalDate.of(2100, 1, 1);
        List<TeacherUnavailability> list;
        if (semesterId != null) {
            list = unavailabilityRepository.searchUnavailabilities(
                    semesterId, teacherId, status, effectiveFrom, effectiveTo);
        } else if (teacherId != null) {
            list = unavailabilityRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        } else {
            list = unavailabilityRepository.findAll();
        }

        Map<Long, String> teacherNames = teacherRepository.findAllById(
                list.stream().map(TeacherUnavailability::getTeacherId).distinct().toList())
                .stream().collect(Collectors.toMap(Teacher::getId, Teacher::getTeacherName));

        return list.stream()
                .map(u -> toDTOWithTeacherName(u, teacherNames.get(u.getTeacherId())))
                .toList();
    }

    private TeacherUnavailability findUnavailability(Long id) {
        return unavailabilityRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký lịch bận"));
    }

    private ResTeacherUnavailabilityDTO toDTO(TeacherUnavailability u) {
        String teacherName = teacherRepository.findById(u.getTeacherId())
                .map(Teacher::getTeacherName).orElse("N/A");
        return toDTOWithTeacherName(u, teacherName);
    }

    private ResTeacherUnavailabilityDTO toDTOWithTeacherName(TeacherUnavailability u, String teacherName) {
        return new ResTeacherUnavailabilityDTO(
                u.getId(),
                u.getTeacherId(),
                teacherName,
                u.getSemesterId(),
                u.getDayOfWeek(),
                u.getSpecificDate(),
                u.getValidFrom(),
                u.getValidTo(),
                u.getSession(),
                u.getPeriodIndexes(),
                u.getNote(),
                u.getStatus(),
                u.getDecisionReason(),
                u.getDecidedBy(),
                u.getDecidedAt(),
                u.getVersion(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }

    private Set<Integer> parsePeriodIndexes(String periodIndexes) {
        if (periodIndexes == null || periodIndexes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(periodIndexes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }
}
