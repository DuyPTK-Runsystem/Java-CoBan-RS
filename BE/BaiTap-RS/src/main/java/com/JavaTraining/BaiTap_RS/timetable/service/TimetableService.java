package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateRevisionDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateTimetableDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqEntryItemDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqUpdateTimetableEntriesDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableDetailDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableEntryDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableReviewDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableSummaryDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableAudit;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadPolicyRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "PMD.CouplingBetweenObjects",
        "PMD.GodClass",
        "PMD.TooManyMethods",
        "PMD.CyclomaticComplexity"
})
public class TimetableService {

    private final TimetableHeadRepository headRepository;
    private final TimetableRevisionRepository revisionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableAuditRepository auditRepository;
    private final SemesterRepository semesterRepository;
    private final TeacherLoadPolicyRepository policyRepository;
    private final SubjectTeachingAssignmentRepository assignmentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final FunctionalRoomRepository roomRepository;
    private final TimetableValidationService validationService;
    private final TimetableCalendarService calendarService;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResTimetableSummaryDTO> pageSummaries(Long semesterId, Pageable pageable) {
        Page<TimetableRevision> page = revisionRepository.findBySemesterId(semesterId, pageable);
        Map<Long, Semester> semesterMap = semesterRepository.findAllById(
                page.getContent().stream().map(TimetableRevision::getSemesterId).distinct().toList())
                .stream().collect(Collectors.toMap(Semester::getId, s -> s));

        Map<Long, TimetableHead> headMap = headRepository.findAllById(
                page.getContent().stream().map(TimetableRevision::getTimetableId).distinct().toList())
                .stream().collect(Collectors.toMap(TimetableHead::getId, h -> h));

        List<ResTimetableSummaryDTO> dtos = page.getContent().stream()
                .map(r -> {
                    Semester s = semesterMap.get(r.getSemesterId());
                    TimetableHead h = headMap.get(r.getTimetableId());
                    return new ResTimetableSummaryDTO(
                            r.getTimetableId(),
                            r.getSemesterId(),
                            s != null ? s.getName() : "N/A",
                            h != null ? h.getCurrentRevisionId() : null,
                            r.getRevisionNumber(),
                            r.getStatus(),
                            r.getEffectiveFrom(),
                            r.getEffectiveTo(),
                            r.getVersion(),
                            h != null ? h.getVersion() : 0L);
                })
                .toList();

        return new ResultPaginationDTO<>(
                new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(),
                        page.getTotalPages(), page.getTotalElements()),
                dtos);
    }

    @Transactional
    public ResTimetableDetailDTO createDraft(ReqCreateTimetableDTO req) {
        Semester semester = semesterRepository.findById(req.semesterId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));

        calendarService.getOrCreateCalendar(req.semesterId());

        TimetableHead head = headRepository.findBySemesterId(req.semesterId())
                .orElseGet(() -> headRepository.save(new TimetableHead(req.semesterId())));

        if (req.expectedHeadVersion() != null && !Objects.equals(head.getVersion(), req.expectedHeadVersion())) {
            throw new AppException(HttpStatus.CONFLICT, "Đầu thời khóa biểu đã bị cập nhật. Vui lòng tải lại.");
        }

        Integer maxRev = revisionRepository.findMaxRevisionNumber(head.getId());
        int nextRev = (maxRev != null ? maxRev : 0) + 1;

        TimetableRevision revision = new TimetableRevision(
                head.getId(),
                req.semesterId(),
                nextRev,
                req.effectiveFrom(),
                req.effectiveTo(),
                req.policyId());
        revision = revisionRepository.save(revision);

        auditRepository.save(new TimetableAudit(
                head.getId(), revision.getId(), "CREATE_DRAFT", AuditContext.currentUserId(),
                "Tạo mới bản nháp revision " + nextRev));

        return toDetailDTO(revision, head, semester);
    }

    @Transactional(readOnly = true)
    public ResTimetableDetailDTO getDetail(Long revisionId) {
        TimetableRevision revision = findRevision(revisionId);
        TimetableHead head = headRepository.findById(revision.getTimetableId()).orElse(null);
        Semester semester = semesterRepository.findById(revision.getSemesterId()).orElse(null);
        return toDetailDTO(revision, head, semester);
    }

    @Transactional(readOnly = true)
    public List<ResTimetableEntryDTO> getEntries(
            Long revisionId,
            LocalDate weekStart,
            Long classId,
            Long teacherId,
            Long functionalRoomId,
            SessionType session,
            Long periodId) {
        TimetableRevision revision = findRevision(revisionId);
        List<TimetableEntry> entries = entryRepository.findByRevisionId(revision.getId());
        if (entries.isEmpty()) {
            return List.of();
        }

        EntryLookupContext ctx = buildLookupContext(entries);
        LocalDate weekEnd = weekStart != null ? weekStart.plusDays(6) : null;

        EntryFilterCriteria filter = new EntryFilterCriteria(
                weekStart, weekEnd, classId, teacherId, functionalRoomId, session, periodId);

        List<ResTimetableEntryDTO> result = new ArrayList<>();
        for (TimetableEntry e : entries) {
            TimetablePeriod p = ctx.periodMap.get(e.getPeriodId());
            SubjectTeachingAssignment a = ctx.assignmentMap.get(e.getAssignmentId());
            ClassSubject cs = a != null ? ctx.classSubjectMap.get(a.getClassSubjectId()) : null;

            if (!matchesFilter(e, p, a, cs, filter)) {
                continue;
            }
            result.add(toEntryDTO(e, p, a, cs, ctx));
        }

        return result;
    }

    @Transactional
    public ResTimetableDetailDTO updateEntries(Long revisionId, ReqUpdateTimetableEntriesDTO req) {
        TimetableRevision revision = findRevision(revisionId);
        validateModifiable(revision, req.expectedVersion());

        if (req.deletedEntryIds() != null && !req.deletedEntryIds().isEmpty()) {
            entryRepository.deleteAllById(req.deletedEntryIds());
        }

        if (req.upserts() != null && !req.upserts().isEmpty()) {
            saveUpsertItems(req.upserts(), revision.getId());
        }

        if (revision.getStatus() == TimetableRevisionStatus.VALIDATED) {
            revision.setStatus(TimetableRevisionStatus.DRAFT);
        }
        revision = revisionRepository.save(revision);

        auditRepository.save(new TimetableAudit(
                revision.getTimetableId(), revision.getId(), "UPDATE_ENTRIES", AuditContext.currentUserId(),
                "Cập nhật danh sách tiết học"));

        TimetableHead head = headRepository.findById(revision.getTimetableId()).orElse(null);
        Semester semester = semesterRepository.findById(revision.getSemesterId()).orElse(null);
        return toDetailDTO(revision, head, semester);
    }

    private void saveUpsertItems(List<ReqEntryItemDTO> upserts, Long revisionId) {
        for (ReqEntryItemDTO item : upserts) {
            processSingleUpsert(item, revisionId);
        }
    }

    private void validateModifiable(TimetableRevision revision, Long expectedVersion) {
        if (revision.getStatus() == TimetableRevisionStatus.PUBLISHED
                || revision.getStatus() == TimetableRevisionStatus.ARCHIVED) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Không thể chỉnh sửa bản thời khóa biểu đã công bố hoặc lưu trữ");
        }
        if (!Objects.equals(revision.getVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Bản thời khóa biểu đã bị sửa đổi bởi người khác. Vui lòng tải lại.");
        }
    }

    private void processSingleUpsert(ReqEntryItemDTO item, Long revisionId) {
        if (item.entryId() != null) {
            TimetableEntry entry = entryRepository.findById(item.entryId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy tiết học"));
            entry.setAssignmentId(item.assignmentId());
            entry.setPeriodId(item.periodId());
            entry.setFunctionalRoomId(item.functionalRoomId());
            entry.setValidFrom(item.validFrom());
            entry.setValidTo(item.validTo());
            entryRepository.save(entry);
        } else {
            TimetableEntry entry = new TimetableEntry(
                    revisionId,
                    item.assignmentId(),
                    item.periodId(),
                    item.functionalRoomId(),
                    item.validFrom(),
                    item.validTo());
            entryRepository.save(entry);
        }
    }

    @Transactional
    public ResTimetableReviewDTO validate(Long revisionId, Long expectedVersion) {
        TimetableRevision revision = findRevision(revisionId);
        if (expectedVersion != null && !Objects.equals(revision.getVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Bản thời khóa biểu đã bị sửa đổi bởi người khác. Vui lòng tải lại.");
        }
        return validationService.validateRevision(revisionId);
    }

    @Transactional(readOnly = true)
    public ResTimetableReviewDTO getReview(Long revisionId) {
        return validationService.validateRevision(revisionId);
    }

    @Transactional
    public ResTimetableDetailDTO createRevision(Long revisionId, ReqCreateRevisionDTO req) {
        TimetableRevision source = findRevision(revisionId);
        if (req.expectedVersion() != null && !Objects.equals(source.getVersion(), req.expectedVersion())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Bản thời khóa biểu đã bị sửa đổi bởi người khác. Vui lòng tải lại.");
        }

        TimetableHead head = headRepository.findById(source.getTimetableId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đầu thời khóa biểu"));

        Integer maxRev = revisionRepository.findMaxRevisionNumber(head.getId());
        int nextRev = (maxRev != null ? maxRev : 0) + 1;

        TimetableRevision newRev = new TimetableRevision(
                head.getId(),
                source.getSemesterId(),
                nextRev,
                req.effectiveFrom(),
                source.getEffectiveTo(),
                source.getPolicyId());
        newRev = revisionRepository.save(newRev);
        final Long newRevId = newRev.getId();

        List<TimetableEntry> sourceEntries = entryRepository.findByRevisionId(source.getId());
        List<TimetableEntry> copiedEntries = sourceEntries.stream()
                .map(e -> new TimetableEntry(
                        newRevId,
                        e.getAssignmentId(),
                        e.getPeriodId(),
                        e.getFunctionalRoomId(),
                        req.effectiveFrom(),
                        e.getValidTo()))
                .toList();
        entryRepository.saveAll(copiedEntries);

        auditRepository.save(new TimetableAudit(
                head.getId(), newRev.getId(), "CREATE_REVISION", AuditContext.currentUserId(),
                "Tạo bản điều chỉnh mới revision " + nextRev + " từ revision " + source.getRevisionNumber()));

        Semester semester = semesterRepository.findById(newRev.getSemesterId()).orElse(null);
        return toDetailDTO(newRev, head, semester);
    }

    private TimetableRevision findRevision(Long id) {
        return revisionRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bản thời khóa biểu"));
    }

    private ResTimetableDetailDTO toDetailDTO(TimetableRevision r, TimetableHead h, Semester s) {
        String policyVersion = null;
        if (r.getPolicyId() != null) {
            policyVersion = policyRepository.findById(r.getPolicyId())
                    .map(TeacherLoadPolicy::getVersion).orElse(null);
        }

        List<String> capabilities = new ArrayList<>();
        if (r.getStatus() == TimetableRevisionStatus.DRAFT) {
            capabilities.add("EDIT_ENTRIES");
            capabilities.add("VALIDATE");
        } else if (r.getStatus() == TimetableRevisionStatus.VALIDATED) {
            capabilities.add("EDIT_ENTRIES");
            capabilities.add("VALIDATE");
            capabilities.add("PUBLISH");
        } else if (r.getStatus() == TimetableRevisionStatus.PUBLISHED) {
            capabilities.add("CREATE_REVISION");
        }

        return new ResTimetableDetailDTO(
                r.getTimetableId(),
                r.getSemesterId(),
                s != null ? s.getName() : "N/A",
                r.getId(),
                r.getRevisionNumber(),
                r.getStatus(),
                r.getEffectiveFrom(),
                r.getEffectiveTo(),
                r.getPolicyId(),
                policyVersion,
                r.getVersion(),
                h != null ? h.getVersion() : 0L,
                r.getBlockingCount(),
                r.getWarningCount(),
                capabilities);
    }

    private boolean matchesFilter(
            TimetableEntry e,
            TimetablePeriod p,
            SubjectTeachingAssignment a,
            ClassSubject cs,
            EntryFilterCriteria filter) {
        return matchesDateRange(e, filter.weekStart(), filter.weekEnd())
                && matchesClassAndTeacher(cs, a, filter.classId(), filter.teacherId())
                && matchesPeriodAndRoom(e, p, filter.functionalRoomId(), filter.session(), filter.periodId());
    }

    private boolean matchesDateRange(TimetableEntry e, LocalDate weekStart, LocalDate weekEnd) {
        return weekStart == null || (!e.getValidFrom().isAfter(weekEnd) && !e.getValidTo().isBefore(weekStart));
    }

    private boolean matchesClassAndTeacher(
            ClassSubject cs,
            SubjectTeachingAssignment a,
            Long classId,
            Long teacherId) {
        boolean classOk = classId == null || (cs != null && Objects.equals(cs.getClassId(), classId));
        boolean teacherOk = teacherId == null || (a != null && Objects.equals(a.getTeacherId(), teacherId));
        return classOk && teacherOk;
    }

    private boolean matchesPeriodAndRoom(
            TimetableEntry e,
            TimetablePeriod p,
            Long functionalRoomId,
            SessionType session,
            Long periodId) {
        if (functionalRoomId != null && !Objects.equals(e.getFunctionalRoomId(), functionalRoomId)) {
            return false;
        }
        if (periodId != null && !Objects.equals(e.getPeriodId(), periodId)) {
            return false;
        }
        return session == null || (p != null && p.getSession() == session);
    }

    private ResTimetableEntryDTO toEntryDTO(
            TimetableEntry e,
            TimetablePeriod p,
            SubjectTeachingAssignment a,
            ClassSubject cs,
            EntryLookupContext ctx) {
        FunctionalRoom room = resolveRoom(e.getFunctionalRoomId(), ctx);
        return new ResTimetableEntryDTO(
                e.getId(),
                e.getRevisionId(),
                e.getAssignmentId(),
                e.getPeriodId(),
                e.getFunctionalRoomId(),
                room != null ? room.getCode() : null,
                room != null ? room.getName() : null,
                resolveClassId(cs),
                resolveClassName(cs, ctx),
                resolveSubjectId(cs),
                resolveSubjectName(cs, ctx),
                resolveTeacherId(a),
                resolveTeacherName(a, ctx),
                e.getValidFrom(),
                e.getValidTo(),
                resolveDayOfWeek(p),
                resolveSession(p),
                resolvePeriodIndex(p),
                resolveStartTime(p),
                resolveEndTime(p));
    }

    private FunctionalRoom resolveRoom(Long roomId, EntryLookupContext ctx) {
        return roomId != null ? ctx.roomMap.get(roomId) : null;
    }

    private Long resolveClassId(ClassSubject cs) {
        return cs != null ? cs.getClassId() : null;
    }

    private String resolveClassName(ClassSubject cs, EntryLookupContext ctx) {
        if (cs == null) {
            return null;
        }
        SchoolClass sc = ctx.classMap.get(cs.getClassId());
        return sc != null ? sc.getClassName() : null;
    }

    private Long resolveSubjectId(ClassSubject cs) {
        return cs != null ? cs.getSubjectId() : null;
    }

    private String resolveSubjectName(ClassSubject cs, EntryLookupContext ctx) {
        if (cs == null) {
            return null;
        }
        Subject subj = ctx.subjectMap.get(cs.getSubjectId());
        return subj != null ? subj.getName() : null;
    }

    private Long resolveTeacherId(SubjectTeachingAssignment a) {
        return a != null ? a.getTeacherId() : null;
    }

    private String resolveTeacherName(SubjectTeachingAssignment a, EntryLookupContext ctx) {
        if (a == null) {
            return null;
        }
        Teacher tch = ctx.teacherMap.get(a.getTeacherId());
        return tch != null ? tch.getTeacherName() : null;
    }

    private Integer resolveDayOfWeek(TimetablePeriod p) {
        return p != null ? p.getDayOfWeek() : null;
    }

    private SessionType resolveSession(TimetablePeriod p) {
        return p != null ? p.getSession() : null;
    }

    private Integer resolvePeriodIndex(TimetablePeriod p) {
        return p != null ? p.getPeriodIndex() : null;
    }

    private LocalTime resolveStartTime(TimetablePeriod p) {
        return p != null ? p.getStartTime() : null;
    }

    private LocalTime resolveEndTime(TimetablePeriod p) {
        return p != null ? p.getEndTime() : null;
    }

    private EntryLookupContext buildLookupContext(List<TimetableEntry> entries) {
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

        return new EntryLookupContext(periodMap, assignmentMap, classSubjectMap,
                classMap, subjectMap, teacherMap, roomMap);
    }

    private record EntryFilterCriteria(
            LocalDate weekStart,
            LocalDate weekEnd,
            Long classId,
            Long teacherId,
            Long functionalRoomId,
            SessionType session,
            Long periodId) {
    }

    private static final class EntryLookupContext {
        private final Map<Long, TimetablePeriod> periodMap;
        private final Map<Long, SubjectTeachingAssignment> assignmentMap;
        private final Map<Long, ClassSubject> classSubjectMap;
        private final Map<Long, SchoolClass> classMap;
        private final Map<Long, Subject> subjectMap;
        private final Map<Long, Teacher> teacherMap;
        private final Map<Long, FunctionalRoom> roomMap;

        private EntryLookupContext(
                Map<Long, TimetablePeriod> periodMap,
                Map<Long, SubjectTeachingAssignment> assignmentMap,
                Map<Long, ClassSubject> classSubjectMap,
                Map<Long, SchoolClass> classMap,
                Map<Long, Subject> subjectMap,
                Map<Long, Teacher> teacherMap,
                Map<Long, FunctionalRoom> roomMap) {
            this.periodMap = periodMap;
            this.assignmentMap = assignmentMap;
            this.classSubjectMap = classSubjectMap;
            this.classMap = classMap;
            this.subjectMap = subjectMap;
            this.teacherMap = teacherMap;
            this.roomMap = roomMap;
        }
    }
}
