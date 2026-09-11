package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableIssueDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableReviewDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailability;
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
public class TimetableValidationService {

    private static final String SEVERITY_BLOCKING = "BLOCKING";
    private static final String SEVERITY_WARNING = "WARNING";

    private final TimetableRevisionRepository revisionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetablePeriodRepository periodRepository;
    private final SubjectTeachingAssignmentRepository assignmentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final FunctionalRoomRepository roomRepository;
    private final SubjectFunctionalRoomRepository subjectFunctionalRoomRepository;
    private final TeacherUnavailabilityRepository unavailabilityRepository;
    private final TeacherLoadEvaluator teacherLoadEvaluator;

    @Transactional
    public ResTimetableReviewDTO validateRevision(Long revisionId) {
        TimetableRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bản thời khóa biểu"));

        List<TimetableEntry> entries = entryRepository.findByRevisionId(revisionId);
        List<ResTimetableIssueDTO> issues = checkAllConflicts(revision, entries);

        LocalDate weekStart = revision.getEffectiveFrom();
        LocalDate weekEnd = weekStart.plusDays(6);
        List<ResTeacherLoadDTO> teacherLoads = teacherLoadEvaluator.evaluateLoads(
                entries, weekStart, weekEnd, revision.getPolicyId());

        appendLoadIssues(teacherLoads, issues);

        int blockingCount = (int) issues.stream().filter(i -> SEVERITY_BLOCKING.equals(i.severity())).count();
        int warningCount = (int) issues.stream().filter(i -> SEVERITY_WARNING.equals(i.severity())).count();

        revision.setBlockingCount(blockingCount);
        revision.setWarningCount(warningCount);
        revision.setValidationFingerprint("val-" + revision.getVersion() + "-" + System.currentTimeMillis());

        if (blockingCount == 0 && revision.getStatus() == TimetableRevisionStatus.DRAFT) {
            revision.setStatus(TimetableRevisionStatus.VALIDATED);
        } else if (blockingCount > 0 && revision.getStatus() == TimetableRevisionStatus.VALIDATED) {
            revision.setStatus(TimetableRevisionStatus.DRAFT);
        }
        revision = revisionRepository.save(revision);

        return new ResTimetableReviewDTO(
                revision.getId(),
                revision.getVersion(),
                LocalDateTime.now(),
                revision.getStatus(),
                blockingCount,
                warningCount,
                issues,
                teacherLoads);
    }

    private void appendLoadIssues(List<ResTeacherLoadDTO> teacherLoads, List<ResTimetableIssueDTO> issues) {
        for (ResTeacherLoadDTO load : teacherLoads) {
            if ("LOAD_UNDETERMINED".equals(load.evaluationStatus())) {
                issues.add(new ResTimetableIssueDTO(
                        "LOAD_UNDETERMINED",
                        SEVERITY_BLOCKING,
                        "Chưa thể xác định định mức cho giáo viên " + load.teacherName()
                                + " do thiếu chính sách định mức.",
                        List.of(), null, null, null, load.teacherName(), null));
            } else if ("LOAD_ABOVE_TARGET".equals(load.evaluationStatus())) {
                issues.add(new ResTimetableIssueDTO(
                        "LOAD_ABOVE_TARGET",
                        SEVERITY_WARNING,
                        "Giáo viên " + load.teacherName() + " được xếp " + load.assignedPeriods()
                                + " tiết, vượt định mức " + load.targetPeriods() + " tiết.",
                        List.of(), null, null, null, load.teacherName(), null));
            } else if ("LOAD_BELOW_TARGET".equals(load.evaluationStatus())) {
                issues.add(new ResTimetableIssueDTO(
                        "LOAD_BELOW_TARGET",
                        SEVERITY_WARNING,
                        "Giáo viên " + load.teacherName() + " được xếp " + load.assignedPeriods()
                                + " tiết, thiếu so với định mức " + load.targetPeriods() + " tiết.",
                        List.of(), null, null, null, load.teacherName(), null));
            }
        }
    }

    public List<ResTimetableIssueDTO> checkAllConflicts(TimetableRevision revision, List<TimetableEntry> entries) {
        List<ResTimetableIssueDTO> issues = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return issues;
        }

        ValidationContext ctx = buildValidationContext(revision, entries);

        for (TimetableEntry entry : entries) {
            validateSingleEntry(entry, ctx, issues);
        }

        int size = entries.size();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                validateCrossOverlap(entries.get(i), entries.get(j), ctx, issues);
            }
        }

        return issues;
    }

    private void validateSingleEntry(
            TimetableEntry entry,
            ValidationContext ctx,
            List<ResTimetableIssueDTO> issues) {
        SubjectTeachingAssignment assignment = ctx.assignmentMap.get(entry.getAssignmentId());
        if (assignment == null || assignment.getStatus() != AssignmentStatus.ACTIVE) {
            issues.add(new ResTimetableIssueDTO(
                    "ASSIGNMENT_INVALID", SEVERITY_BLOCKING,
                    "Phân công giảng dạy không hợp lệ hoặc đã ngưng hoạt động",
                    List.of(entry.getId()), entry.getPeriodId(), null, null, null, null));
        } else if (assignment.getValidFrom().isAfter(entry.getValidFrom())
                || (assignment.getValidTo() != null && assignment.getValidTo().isBefore(entry.getValidTo()))) {
            issues.add(new ResTimetableIssueDTO(
                    "ASSIGNMENT_INVALID", SEVERITY_BLOCKING,
                    "Khoảng thời gian của tiết học vượt ngoài hiệu lực của phân công giảng dạy",
                    List.of(entry.getId()), entry.getPeriodId(), null, null, null, null));
        }

        validateFunctionalRoom(entry, assignment, ctx, issues);
        validateTeacherAvailability(entry, assignment, ctx, issues);
    }

    private void validateFunctionalRoom(
            TimetableEntry entry,
            SubjectTeachingAssignment assignment,
            ValidationContext ctx,
            List<ResTimetableIssueDTO> issues) {
        if (entry.getFunctionalRoomId() == null) {
            return;
        }
        FunctionalRoom room = ctx.roomMap.get(entry.getFunctionalRoomId());
        if (room == null || room.getStatus() != RoomStatus.ACTIVE) {
            issues.add(new ResTimetableIssueDTO(
                    "FUNCTIONAL_ROOM_INVALID", SEVERITY_BLOCKING,
                    "Phòng chức năng không tồn tại hoặc không còn hoạt động",
                    List.of(entry.getId()), entry.getPeriodId(), null, null, null,
                    room != null ? room.getName() : null));
            return;
        }
        if (assignment != null) {
            ClassSubject cs = ctx.classSubjectMap.get(assignment.getClassSubjectId());
            if (cs != null && !subjectFunctionalRoomRepository
                    .existsBySubjectIdAndFunctionalRoomId(cs.getSubjectId(), entry.getFunctionalRoomId())) {
                Subject subj = ctx.subjectMap.get(cs.getSubjectId());
                issues.add(new ResTimetableIssueDTO(
                        "FUNCTIONAL_ROOM_INVALID", SEVERITY_BLOCKING,
                        "Phòng chức năng '" + room.getName() + "' không được gán cho môn học '"
                                + (subj != null ? subj.getName() : "") + "'",
                        List.of(entry.getId()), entry.getPeriodId(), null, null, null, room.getName()));
            }
        }
    }

    private void validateTeacherAvailability(
            TimetableEntry entry,
            SubjectTeachingAssignment assignment,
            ValidationContext ctx,
            List<ResTimetableIssueDTO> issues) {
        if (assignment == null) {
            return;
        }
        TimetablePeriod period = ctx.periodMap.get(entry.getPeriodId());
        Teacher teacher = ctx.teacherMap.get(assignment.getTeacherId());
        for (TeacherUnavailability unav : ctx.approvedUnavailabilities) {
            if (Objects.equals(unav.getTeacherId(), assignment.getTeacherId())
                    && datesOverlap(entry.getValidFrom(), entry.getValidTo(), unav.getValidFrom(), unav.getValidTo())
                    && period != null && periodMatchesUnavailability(period, unav)) {
                issues.add(new ResTimetableIssueDTO(
                        "TEACHER_UNAVAILABLE", SEVERITY_BLOCKING,
                        "Tiết học trùng với lịch bận đã duyệt của giáo viên "
                                + (teacher != null ? teacher.getTeacherName() : ""),
                        List.of(entry.getId()), entry.getPeriodId(), null, null,
                        teacher != null ? teacher.getTeacherName() : null, null));
            }
        }
    }

    private void validateCrossOverlap(
            TimetableEntry e1,
            TimetableEntry e2,
            ValidationContext ctx,
            List<ResTimetableIssueDTO> issues) {
        TimetablePeriod p1 = ctx.periodMap.get(e1.getPeriodId());
        TimetablePeriod p2 = ctx.periodMap.get(e2.getPeriodId());
        if (!periodsOverlap(p1, p2)
                || !datesOverlap(e1.getValidFrom(), e1.getValidTo(), e2.getValidFrom(), e2.getValidTo())) {
            return;
        }

        SubjectTeachingAssignment a1 = ctx.assignmentMap.get(e1.getAssignmentId());
        SubjectTeachingAssignment a2 = ctx.assignmentMap.get(e2.getAssignmentId());
        ClassSubject cs1 = a1 != null ? ctx.classSubjectMap.get(a1.getClassSubjectId()) : null;
        ClassSubject cs2 = a2 != null ? ctx.classSubjectMap.get(a2.getClassSubjectId()) : null;

        checkClassOverlap(e1, e2, cs1, cs2, ctx, issues);
        checkTeacherOverlap(e1, e2, a1, a2, ctx, issues);
        checkRoomOverlap(e1, e2, ctx, issues);
    }

    private void checkClassOverlap(
            TimetableEntry e1, TimetableEntry e2,
            ClassSubject cs1, ClassSubject cs2,
            ValidationContext ctx, List<ResTimetableIssueDTO> issues) {
        if (cs1 != null && cs2 != null && Objects.equals(cs1.getClassId(), cs2.getClassId())) {
            SchoolClass sc = ctx.classMap.get(cs1.getClassId());
            String className = sc != null ? sc.getClassName() : "N/A";
            issues.add(new ResTimetableIssueDTO(
                    "CLASS_OVERLAP", SEVERITY_BLOCKING,
                    "Lớp " + className + " bị trùng 2 tiết học vào cùng khung giờ",
                    List.of(e1.getId(), e2.getId()), e1.getPeriodId(), null, className, null, null));
        }
    }

    private void checkTeacherOverlap(
            TimetableEntry e1, TimetableEntry e2,
            SubjectTeachingAssignment a1, SubjectTeachingAssignment a2,
            ValidationContext ctx, List<ResTimetableIssueDTO> issues) {
        if (a1 != null && a2 != null && Objects.equals(a1.getTeacherId(), a2.getTeacherId())) {
            Teacher teacher = ctx.teacherMap.get(a1.getTeacherId());
            String teacherName = teacher != null ? teacher.getTeacherName() : "N/A";
            issues.add(new ResTimetableIssueDTO(
                    "TEACHER_OVERLAP", SEVERITY_BLOCKING,
                    "Giáo viên " + teacherName + " bị trùng lịch dạy ở 2 lớp vào cùng khung giờ",
                    List.of(e1.getId(), e2.getId()), e1.getPeriodId(), null, null, teacherName, null));
        }
    }

    private void checkRoomOverlap(
            TimetableEntry e1, TimetableEntry e2,
            ValidationContext ctx, List<ResTimetableIssueDTO> issues) {
        if (e1.getFunctionalRoomId() != null && e2.getFunctionalRoomId() != null
                && Objects.equals(e1.getFunctionalRoomId(), e2.getFunctionalRoomId())) {
            FunctionalRoom room = ctx.roomMap.get(e1.getFunctionalRoomId());
            String roomName = room != null ? room.getName() : "N/A";
            issues.add(new ResTimetableIssueDTO(
                    "ROOM_OVERLAP", SEVERITY_BLOCKING,
                    "Phòng chức năng " + roomName + " bị trùng tiết sử dụng vào cùng khung giờ",
                    List.of(e1.getId(), e2.getId()), e1.getPeriodId(), null, null, null, roomName));
        }
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    private boolean periodsOverlap(TimetablePeriod p1, TimetablePeriod p2) {
        if (p1 == null || p2 == null || !Objects.equals(p1.getDayOfWeek(), p2.getDayOfWeek())) {
            return false;
        }
        return p1.getStartTime().isBefore(p2.getEndTime()) && p1.getEndTime().isAfter(p2.getStartTime());
    }

    private boolean periodMatchesUnavailability(TimetablePeriod period, TeacherUnavailability unav) {
        if (unav.getDayOfWeek() != null && !Objects.equals(unav.getDayOfWeek(), period.getDayOfWeek())) {
            return false;
        }
        if (unav.getSession() != null && unav.getSession() != period.getSession()) {
            return false;
        }
        if (unav.getPeriodIndexes() != null && !unav.getPeriodIndexes().isBlank()) {
            Set<Integer> indexes = Arrays.stream(unav.getPeriodIndexes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            return indexes.contains(period.getPeriodIndex());
        }
        return true;
    }

    private ValidationContext buildValidationContext(TimetableRevision revision, List<TimetableEntry> entries) {
        Map<Long, TimetablePeriod> periodMap = periodRepository.findAllById(
                entries.stream().map(TimetableEntry::getPeriodId).distinct().toList())
                .stream().collect(Collectors.toMap(TimetablePeriod::getId, p -> p));

        Map<Long, SubjectTeachingAssignment> assignmentMap = assignmentRepository.findAllById(
                entries.stream().map(TimetableEntry::getAssignmentId).distinct().toList())
                .stream().collect(Collectors.toMap(SubjectTeachingAssignment::getId, a -> a));

        Map<Long, ClassSubject> classSubjectMap = classSubjectRepository.findAllById(
                assignmentMap.values().stream().map(SubjectTeachingAssignment::getClassSubjectId).distinct().toList())
                .stream().collect(Collectors.toMap(ClassSubject::getId, cs -> cs));

        Map<Long, SchoolClass> classMap = schoolClassRepository.findAllById(
                classSubjectMap.values().stream().map(ClassSubject::getClassId).distinct().toList())
                .stream().collect(Collectors.toMap(SchoolClass::getId, c -> c));

        Map<Long, Subject> subjectMap = subjectRepository.findAllById(
                classSubjectMap.values().stream().map(ClassSubject::getSubjectId).distinct().toList())
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));

        Map<Long, Teacher> teacherMap = teacherRepository.findAllById(
                assignmentMap.values().stream().map(SubjectTeachingAssignment::getTeacherId).distinct().toList())
                .stream().collect(Collectors.toMap(Teacher::getId, t -> t));

        Map<Long, FunctionalRoom> roomMap = roomRepository.findAllById(
                entries.stream().map(TimetableEntry::getFunctionalRoomId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(FunctionalRoom::getId, r -> r));

        LocalDate effectiveTo = revision.getEffectiveTo() != null
                ? revision.getEffectiveTo()
                : LocalDate.of(2100, 1, 1);
        List<TeacherUnavailability> approvedUnavailabilities = unavailabilityRepository
                .findApprovedInSemester(revision.getSemesterId(), revision.getEffectiveFrom(), effectiveTo);

        return new ValidationContext(periodMap, assignmentMap, classSubjectMap,
                classMap, subjectMap, teacherMap, roomMap, approvedUnavailabilities);
    }

    private static final class ValidationContext {
        private final Map<Long, TimetablePeriod> periodMap;
        private final Map<Long, SubjectTeachingAssignment> assignmentMap;
        private final Map<Long, ClassSubject> classSubjectMap;
        private final Map<Long, SchoolClass> classMap;
        private final Map<Long, Subject> subjectMap;
        private final Map<Long, Teacher> teacherMap;
        private final Map<Long, FunctionalRoom> roomMap;
        private final List<TeacherUnavailability> approvedUnavailabilities;

        private ValidationContext(
                Map<Long, TimetablePeriod> periodMap,
                Map<Long, SubjectTeachingAssignment> assignmentMap,
                Map<Long, ClassSubject> classSubjectMap,
                Map<Long, SchoolClass> classMap,
                Map<Long, Subject> subjectMap,
                Map<Long, Teacher> teacherMap,
                Map<Long, FunctionalRoom> roomMap,
                List<TeacherUnavailability> approvedUnavailabilities) {
            this.periodMap = periodMap;
            this.assignmentMap = assignmentMap;
            this.classSubjectMap = classSubjectMap;
            this.classMap = classMap;
            this.subjectMap = subjectMap;
            this.teacherMap = teacherMap;
            this.roomMap = roomMap;
            this.approvedUnavailabilities = approvedUnavailabilities;
        }
    }
}