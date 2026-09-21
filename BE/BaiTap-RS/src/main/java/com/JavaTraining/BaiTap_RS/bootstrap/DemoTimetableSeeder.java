package com.JavaTraining.BaiTap_RS.bootstrap;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableAudit;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@SuppressWarnings({
        "PMD.CyclomaticComplexity",
        "PMD.ExcessiveImports",
        "PMD.CouplingBetweenObjects",
        "PMD.TooManyMethods"
})
public class DemoTimetableSeeder {

    private static final String TIMETABLE_SEED_ACTION = "SEED_PLAN_081_G7";
    private static final Set<String> TARGET_CLASS_CODES = Set.of("7A1", "7A2", "7A3", "7A4");
    private static final Map<String, List<SlotSeed>> SCHEDULES = buildSchedules();

    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectTeachingAssignmentRepository assignmentRepository;
    private final TimetableCalendarRepository calendarRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableHeadRepository headRepository;
    private final TimetableRevisionRepository revisionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetableAuditRepository auditRepository;

    public DemoTimetableSeeder(
            ClassSubjectRepository classSubjectRepository,
            SubjectRepository subjectRepository,
            SubjectTeachingAssignmentRepository assignmentRepository,
            TimetableCalendarRepository calendarRepository,
            TimetablePeriodRepository periodRepository,
            TimetableHeadRepository headRepository,
            TimetableRevisionRepository revisionRepository,
            TimetableEntryRepository entryRepository,
            TimetableAuditRepository auditRepository) {
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.assignmentRepository = assignmentRepository;
        this.calendarRepository = calendarRepository;
        this.periodRepository = periodRepository;
        this.headRepository = headRepository;
        this.revisionRepository = revisionRepository;
        this.entryRepository = entryRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void seed(List<SchoolClass> classes, List<Semester> semesters) {
        Map<String, SchoolClass> classesByCode = classes.stream()
                .filter(schoolClass -> TARGET_CLASS_CODES.contains(schoolClass.getClassCode()))
                .collect(Collectors.toMap(
                        SchoolClass::getClassCode,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, Semester> semestersByCode = semesters.stream()
                .collect(Collectors.toMap(Semester::getCode, Function.identity(), (left, right) -> left));
        Map<String, Subject> subjectsByCode = subjectRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(Subject::getCode, Function.identity()));
        for (String semesterCode : List.of("HK1", "HK2")) {
            Semester semester = requireSemester(semestersByCode, semesterCode);
            TimetableHead head = headRepository.findBySemesterId(semester.getId()).orElse(null);
            if (hasSeededRevision(head) || hasExistingEntries(head)) {
                continue;
            }
            TimetableCalendar calendar = ensureCalendar(semester.getId());
            Map<PeriodKey, TimetablePeriod> periods = ensurePeriods(
                    calendar.getId(), schedulesForSemester(semesterCode));
            TimetableRevision revision = createRevision(semester, head);
            List<PlannedEntry> plannedEntries = buildEntries(
                    semester, classesByCode, subjectsByCode, periods);
            assertNoConflicts(plannedEntries);
            head = headRepository.findBySemesterId(semester.getId())
                    .orElseThrow(() -> new IllegalStateException("Timetable head was not created"));
            entryRepository.saveAll(plannedEntries.stream()
                    .map(entry -> new TimetableEntry(
                            revision.getId(),
                            entry.assignmentId(),
                            entry.periodId(),
                            entry.functionalRoomId(),
                            semester.getStartDate(),
                            semester.getEndDate()))
                    .toList());
            auditRepository.save(new TimetableAudit(
                    head.getId(),
                    revision.getId(),
                    TIMETABLE_SEED_ACTION,
                    null,
                    "seedKey=PLAN-081-GRADE-7;semester=" + semesterCode
                            + ";classes=7A1,7A2,7A3,7A4"));
            head.setCurrentRevisionId(revision.getId());
            headRepository.save(head);
        }
    }

    private Semester requireSemester(Map<String, Semester> semestersByCode, String code) {
        Semester semester = semestersByCode.get(code);
        LocalDate expectedStart = "HK1".equals(code)
                ? LocalDate.of(2026, 9, 1)
                : LocalDate.of(2027, 1, 1);
        LocalDate expectedEnd = "HK1".equals(code)
                ? LocalDate.of(2026, 12, 31)
                : LocalDate.of(2027, 5, 31);
        if (semester == null || !expectedStart.equals(semester.getStartDate())
                || !expectedEnd.equals(semester.getEndDate())) {
            throw new IllegalStateException("Semester " + code + " does not have the Plan 081 date range");
        }
        return semester;
    }

    private boolean hasSeededRevision(TimetableHead head) {
        if (head == null) {
            return false;
        }
        return revisionRepository.findByTimetableIdOrderByRevisionNumberDesc(head.getId()).stream()
                .anyMatch(revision -> auditRepository.existsByRevisionIdAndAction(
                        revision.getId(), TIMETABLE_SEED_ACTION));
    }

    private boolean hasExistingEntries(TimetableHead head) {
        if (head == null) {
            return false;
        }
        return revisionRepository.findByTimetableIdOrderByRevisionNumberDesc(head.getId()).stream()
                .anyMatch(revision -> !entryRepository.findByRevisionId(revision.getId()).isEmpty());
    }

    private TimetableCalendar ensureCalendar(Long semesterId) {
        return calendarRepository.findBySemesterId(semesterId)
                .orElseGet(() -> calendarRepository.save(new TimetableCalendar(semesterId)));
    }

    private Map<PeriodKey, TimetablePeriod> ensurePeriods(
            Long calendarId,
            List<SlotSeed> schedule) {
        Map<PeriodKey, TimetablePeriod> periods = periodRepository
                .findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(calendarId).stream()
                .collect(Collectors.toMap(
                        period -> new PeriodKey(period.getDayOfWeek(), period.getSession(), period.getPeriodIndex()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        schedule.stream()
                .map(slot -> new PeriodKey(slot.dayOfWeek(), slot.session(), slot.periodIndex()))
                .distinct()
                .forEach(key -> periods.computeIfAbsent(key, periodKey -> periodRepository.save(
                        new TimetablePeriod(
                                calendarId,
                                periodKey.dayOfWeek(),
                                periodKey.session(),
                                periodKey.periodIndex(),
                                periodName(periodKey),
                                periodStart(periodKey),
                                periodStart(periodKey).plusMinutes(45)))));
        return periods;
    }

    private TimetableRevision createRevision(Semester semester, TimetableHead existingHead) {
        TimetableHead head = existingHead != null
                ? existingHead
                : headRepository.save(new TimetableHead(semester.getId()));
        Integer maxRevision = revisionRepository.findMaxRevisionNumber(head.getId());
        return revisionRepository.save(new TimetableRevision(
                head.getId(),
                semester.getId(),
                maxRevision == null ? 1 : maxRevision + 1,
                semester.getStartDate(),
                semester.getEndDate(),
                null));
    }

    private List<PlannedEntry> buildEntries(
            Semester semester,
            Map<String, SchoolClass> classesByCode,
            Map<String, Subject> subjectsByCode,
            Map<PeriodKey, TimetablePeriod> periods) {
        List<PlannedEntry> entries = new ArrayList<>();
        TARGET_CLASS_CODES.stream().sorted().forEach(classCode -> {
            SchoolClass schoolClass = classesByCode.get(classCode);
            if (schoolClass == null) {
                throw new IllegalStateException("Missing target class: " + classCode);
            }
            Map<Long, ClassSubject> classSubjectsBySubjectId = classSubjectRepository
                    .findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(schoolClass.getId(), semester.getId()).stream()
                    .filter(classSubject -> classSubject.getStatus() == ClassSubjectStatus.ACTIVE)
                    .collect(Collectors.toMap(ClassSubject::getSubjectId, Function.identity()));
            for (SlotSeed slot : SCHEDULES.get(scheduleKey(semester.getCode(), classCode))) {
                Subject subject = subjectsByCode.get(slot.subjectCode());
                if (subject == null) {
                    throw new IllegalStateException("Missing subject: " + slot.subjectCode());
                }
                ClassSubject classSubject = classSubjectsBySubjectId.get(subject.getId());
                if (classSubject == null) {
                    throw new IllegalStateException("Missing active class_subject for "
                            + classCode + "/" + semester.getCode() + "/" + slot.subjectCode());
                }
                SubjectTeachingAssignment assignment = assignmentRepository
                        .findFirstByClassSubjectIdAndStatus(classSubject.getId(), AssignmentStatus.ACTIVE)
                        .orElseThrow(() -> new IllegalStateException("Missing active assignment for "
                                + classCode + "/" + semester.getCode() + "/" + slot.subjectCode()));
                if (assignment.getValidFrom().isAfter(semester.getStartDate())
                        || (assignment.getValidTo() != null
                                && assignment.getValidTo().isBefore(semester.getEndDate()))) {
                    throw new IllegalStateException("Assignment date range does not cover semester for "
                            + classCode + "/" + semester.getCode() + "/" + slot.subjectCode());
                }
                TimetablePeriod period = periods.get(
                        new PeriodKey(slot.dayOfWeek(), slot.session(), slot.periodIndex()));
                if (period == null) {
                    throw new IllegalStateException("Missing timetable period for seed slot");
                }
                entries.add(new PlannedEntry(
                        schoolClass.getId(),
                        assignment.getId(),
                        assignment.getTeacherId(),
                        period.getId(),
                        null));
            }
        });
        return entries;
    }

    private void assertNoConflicts(List<PlannedEntry> entries) {
        Map<String, PlannedEntry> classSlots = new HashMap<>();
        Map<String, PlannedEntry> teacherSlots = new HashMap<>();
        Map<String, PlannedEntry> roomSlots = new HashMap<>();
        for (PlannedEntry entry : entries) {
            requireUnique(classSlots, entry.classId() + "|" + entry.periodId(), "class");
            requireUnique(teacherSlots, entry.teacherId() + "|" + entry.periodId(), "teacher");
            if (entry.functionalRoomId() != null) {
                requireUnique(roomSlots, entry.functionalRoomId() + "|" + entry.periodId(), "room");
            }
        }
    }

    private void requireUnique(Map<String, PlannedEntry> occupied, String key, String resource) {
        if (occupied.putIfAbsent(key, new PlannedEntry(null, null, null, null, null)) != null) {
            throw new IllegalStateException("Plan 081 timetable " + resource + " conflict at " + key);
        }
    }

    private List<SlotSeed> schedulesForSemester(String semesterCode) {
        return SCHEDULES.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(semesterCode + "|"))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

    private String periodName(PeriodKey key) {
        return (key.session() == SessionType.MORNING ? "Sáng" : "Chiều")
                + " Tiết " + key.periodIndex();
    }

    private LocalTime periodStart(PeriodKey key) {
        int startHour = key.session() == SessionType.MORNING ? 7 : 13;
        int[] minuteOffsets = { 0, 50, 110, 160 };
        return LocalTime.of(startHour, 0).plusMinutes(minuteOffsets[key.periodIndex() - 1]);
    }

    private static String scheduleKey(String semesterCode, String classCode) {
        return semesterCode + "|" + classCode;
    }

    private static Map<String, List<SlotSeed>> buildSchedules() {
        Map<String, List<SlotSeed>> schedules = new LinkedHashMap<>();
        schedules.put(scheduleKey("HK1", "7A1"), schedule(
                slots("LICH_SU", 2, SessionType.MORNING, 3),
                slots("GDCD", 2, SessionType.MORNING, 4),
                slots("NGU_VAN", 2, SessionType.AFTERNOON, 1, 2),
                slots("TOAN", 2, SessionType.AFTERNOON, 3, 4),
                slots("DIA_LY", 3, SessionType.MORNING, 1),
                slots("CONG_NGHE", 3, SessionType.MORNING, 2),
                slots("NGOAI_NGU", 3, SessionType.MORNING, 3),
                slots("TIN_HOC", 4, SessionType.MORNING, 1, 2),
                slots("TOAN", 4, SessionType.MORNING, 3, 4),
                slots("DIA_LY", 4, SessionType.AFTERNOON, 3),
                slots("NGOAI_NGU", 4, SessionType.AFTERNOON, 4),
                slots("NGU_VAN", 5, SessionType.AFTERNOON, 1, 2),
                slots("SINH_HOC", 5, SessionType.AFTERNOON, 3, 4),
                slots("VAT_LY", 6, SessionType.MORNING, 2, 3),
                slots("CONG_NGHE", 6, SessionType.MORNING, 4),
                slots("NGOAI_NGU", 6, SessionType.AFTERNOON, 1),
                slots("LICH_SU", 6, SessionType.AFTERNOON, 2)));
        schedules.put(scheduleKey("HK1", "7A2"), schedule(
                slots("NGU_VAN", 2, SessionType.MORNING, 3, 4),
                slots("NGOAI_NGU", 3, SessionType.MORNING, 2),
                slots("DIA_LY", 3, SessionType.MORNING, 3),
                slots("CONG_NGHE", 3, SessionType.MORNING, 4),
                slots("SINH_HOC", 3, SessionType.AFTERNOON, 1, 2),
                slots("TOAN", 3, SessionType.AFTERNOON, 3, 4),
                slots("NGOAI_NGU", 4, SessionType.MORNING, 1),
                slots("LICH_SU", 4, SessionType.MORNING, 2),
                slots("NGU_VAN", 4, SessionType.MORNING, 3, 4),
                slots("TIN_HOC", 4, SessionType.AFTERNOON, 1, 2),
                slots("VAT_LY", 4, SessionType.AFTERNOON, 3, 4),
                slots("TOAN", 6, SessionType.MORNING, 2, 3),
                slots("GDCD", 6, SessionType.MORNING, 4),
                slots("NGOAI_NGU", 6, SessionType.AFTERNOON, 1),
                slots("CONG_NGHE", 6, SessionType.AFTERNOON, 2),
                slots("LICH_SU", 6, SessionType.AFTERNOON, 3),
                slots("DIA_LY", 6, SessionType.AFTERNOON, 4)));
        schedules.put(scheduleKey("HK1", "7A3"), schedule(
                slots("TOAN", 2, SessionType.MORNING, 3, 4),
                slots("CONG_NGHE", 2, SessionType.AFTERNOON, 1),
                slots("NGOAI_NGU", 2, SessionType.AFTERNOON, 2),
                slots("GDCD", 2, SessionType.AFTERNOON, 3),
                slots("NGU_VAN", 3, SessionType.MORNING, 1, 2),
                slots("VAT_LY", 3, SessionType.MORNING, 3, 4),
                slots("DIA_LY", 3, SessionType.AFTERNOON, 3),
                slots("LICH_SU", 3, SessionType.AFTERNOON, 4),
                slots("DIA_LY", 4, SessionType.MORNING, 1),
                slots("NGOAI_NGU", 4, SessionType.MORNING, 2),
                slots("NGU_VAN", 4, SessionType.AFTERNOON, 1, 2),
                slots("SINH_HOC", 4, SessionType.AFTERNOON, 3, 4),
                slots("NGOAI_NGU", 6, SessionType.MORNING, 2),
                slots("CONG_NGHE", 6, SessionType.MORNING, 3),
                slots("LICH_SU", 6, SessionType.MORNING, 4),
                slots("TOAN", 6, SessionType.AFTERNOON, 1, 2),
                slots("TIN_HOC", 6, SessionType.AFTERNOON, 3, 4)));
        schedules.put(scheduleKey("HK1", "7A4"), schedule(
                slots("NGU_VAN", 2, SessionType.MORNING, 3, 4),
                slots("NGOAI_NGU", 3, SessionType.MORNING, 1),
                slots("DIA_LY", 3, SessionType.MORNING, 2),
                slots("CONG_NGHE", 3, SessionType.MORNING, 3),
                slots("LICH_SU", 4, SessionType.AFTERNOON, 1),
                slots("NGOAI_NGU", 4, SessionType.AFTERNOON, 2),
                slots("TOAN", 4, SessionType.AFTERNOON, 3, 4),
                slots("NGOAI_NGU", 5, SessionType.MORNING, 1),
                slots("LICH_SU", 5, SessionType.MORNING, 2),
                slots("CONG_NGHE", 5, SessionType.MORNING, 3),
                slots("GDCD", 5, SessionType.MORNING, 4),
                slots("NGU_VAN", 5, SessionType.AFTERNOON, 1, 2),
                slots("TOAN", 5, SessionType.AFTERNOON, 3, 4),
                slots("DIA_LY", 6, SessionType.MORNING, 2),
                slots("SINH_HOC", 6, SessionType.MORNING, 3, 4),
                slots("TIN_HOC", 6, SessionType.AFTERNOON, 1, 2),
                slots("VAT_LY", 6, SessionType.AFTERNOON, 3, 4)));
        schedules.put(scheduleKey("HK2", "7A1"), schedule(
                slots("TOAN", 2, SessionType.MORNING, 3, 4),
                slots("NGU_VAN", 2, SessionType.AFTERNOON, 1, 2),
                slots("SINH_HOC", 2, SessionType.AFTERNOON, 3, 4),
                slots("LICH_SU", 3, SessionType.MORNING, 2),
                slots("NGOAI_NGU", 3, SessionType.MORNING, 3),
                slots("CONG_NGHE", 3, SessionType.MORNING, 4),
                slots("LICH_SU", 4, SessionType.MORNING, 3),
                slots("DIA_LY", 4, SessionType.MORNING, 4),
                slots("NGOAI_NGU", 5, SessionType.MORNING, 1),
                slots("VAT_LY", 5, SessionType.MORNING, 2),
                slots("TOAN", 5, SessionType.AFTERNOON, 1, 2),
                slots("TIN_HOC", 5, SessionType.AFTERNOON, 3, 4),
                slots("NGOAI_NGU", 6, SessionType.MORNING, 2),
                slots("GDCD", 6, SessionType.MORNING, 3),
                slots("VAT_LY", 6, SessionType.MORNING, 4),
                slots("CONG_NGHE", 6, SessionType.AFTERNOON, 1),
                slots("DIA_LY", 6, SessionType.AFTERNOON, 2),
                slots("NGU_VAN", 6, SessionType.AFTERNOON, 3, 4)));
        schedules.put(scheduleKey("HK2", "7A2"), schedule(
                slots("CONG_NGHE", 2, SessionType.MORNING, 3),
                slots("NGU_VAN", 2, SessionType.MORNING, 4),
                slots("SINH_HOC", 2, SessionType.AFTERNOON, 1, 2),
                slots("DIA_LY", 2, SessionType.AFTERNOON, 3),
                slots("NGOAI_NGU", 2, SessionType.AFTERNOON, 4),
                slots("NGU_VAN", 3, SessionType.MORNING, 1, 2),
                slots("TOAN", 3, SessionType.MORNING, 3, 4),
                slots("NGOAI_NGU", 4, SessionType.MORNING, 1),
                slots("LICH_SU", 4, SessionType.MORNING, 2),
                slots("GDCD", 4, SessionType.MORNING, 3),
                slots("TIN_HOC", 4, SessionType.AFTERNOON, 1, 2),
                slots("VAT_LY", 4, SessionType.AFTERNOON, 3, 4),
                slots("CONG_NGHE", 5, SessionType.AFTERNOON, 3),
                slots("NGOAI_NGU", 5, SessionType.AFTERNOON, 4),
                slots("TOAN", 6, SessionType.MORNING, 2, 3),
                slots("NGU_VAN", 6, SessionType.MORNING, 4),
                slots("LICH_SU", 6, SessionType.AFTERNOON, 3),
                slots("DIA_LY", 6, SessionType.AFTERNOON, 4)));
        schedules.put(scheduleKey("HK2", "7A3"), schedule(
                slots("LICH_SU", 2, SessionType.MORNING, 3),
                slots("DIA_LY", 2, SessionType.MORNING, 4),
                slots("TIN_HOC", 2, SessionType.AFTERNOON, 1, 2),
                slots("TOAN", 2, SessionType.AFTERNOON, 3, 4),
                slots("TOAN", 3, SessionType.MORNING, 1, 2),
                slots("SINH_HOC", 3, SessionType.MORNING, 3, 4),
                slots("VAT_LY", 3, SessionType.AFTERNOON, 1, 2),
                slots("CONG_NGHE", 3, SessionType.AFTERNOON, 3),
                slots("NGOAI_NGU", 3, SessionType.AFTERNOON, 4),
                slots("NGU_VAN", 4, SessionType.AFTERNOON, 1, 2),
                slots("NGOAI_NGU", 4, SessionType.AFTERNOON, 3),
                slots("NGU_VAN", 5, SessionType.AFTERNOON, 1, 2),
                slots("CONG_NGHE", 5, SessionType.AFTERNOON, 3),
                slots("LICH_SU", 5, SessionType.AFTERNOON, 4),
                slots("DIA_LY", 6, SessionType.MORNING, 2),
                slots("NGOAI_NGU", 6, SessionType.MORNING, 3),
                slots("GDCD", 6, SessionType.MORNING, 4)));
        schedules.put(scheduleKey("HK2", "7A4"), schedule(
                slots("NGOAI_NGU", 2, SessionType.MORNING, 3),
                slots("LICH_SU", 2, SessionType.MORNING, 4),
                slots("NGU_VAN", 3, SessionType.MORNING, 1, 2),
                slots("TIN_HOC", 3, SessionType.MORNING, 3, 4),
                slots("LICH_SU", 3, SessionType.AFTERNOON, 1),
                slots("DIA_LY", 3, SessionType.AFTERNOON, 2),
                slots("TOAN", 3, SessionType.AFTERNOON, 3, 4),
                slots("VAT_LY", 4, SessionType.MORNING, 1, 2),
                slots("TOAN", 4, SessionType.MORNING, 3, 4),
                slots("SINH_HOC", 4, SessionType.AFTERNOON, 2),
                slots("NGOAI_NGU", 4, SessionType.AFTERNOON, 3),
                slots("CONG_NGHE", 4, SessionType.AFTERNOON, 4),
                slots("GDCD", 5, SessionType.MORNING, 3),
                slots("CONG_NGHE", 5, SessionType.MORNING, 4),
                slots("NGOAI_NGU", 5, SessionType.AFTERNOON, 3),
                slots("DIA_LY", 5, SessionType.AFTERNOON, 4),
                slots("NGU_VAN", 6, SessionType.MORNING, 2, 3),
                slots("SINH_HOC", 6, SessionType.MORNING, 4)));
        return Map.copyOf(schedules);
    }

    private static List<SlotSeed> schedule(SlotSeed[]... groups) {
        List<SlotSeed> result = new ArrayList<>();
        for (SlotSeed[] group : groups) {
            result.addAll(List.of(group));
        }
        return List.copyOf(result);
    }

    private static SlotSeed[] slots(
            String subjectCode,
            int dayOfWeek,
            SessionType session,
            int... periodIndexes) {
        if (dayOfWeek < 2 || dayOfWeek > 6) {
            throw new IllegalArgumentException("Plan 081 timetable only supports Monday-Friday");
        }
        SlotSeed[] result = new SlotSeed[periodIndexes.length];
        for (int index = 0; index < periodIndexes.length; index++) {
            result[index] = new SlotSeed(subjectCode, dayOfWeek, session, periodIndexes[index]);
        }
        return result;
    }

    private record SlotSeed(String subjectCode, int dayOfWeek, SessionType session, int periodIndex) {
    }

    private record PeriodKey(int dayOfWeek, SessionType session, int periodIndex) {
    }

    private record PlannedEntry(Long classId, Long assignmentId, Long teacherId, Long periodId,
            Long functionalRoomId) {
    }
}
