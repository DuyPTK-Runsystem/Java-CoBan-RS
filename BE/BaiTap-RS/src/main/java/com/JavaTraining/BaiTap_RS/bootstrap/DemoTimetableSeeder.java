package com.JavaTraining.BaiTap_RS.bootstrap;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.bootstrap.DemoTimetableScheduleCatalog.SlotSeed;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableAudit;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
// This fixture orchestrator intentionally coordinates timetable repositories and keeps
// schedule-specific helper methods together. The fixture data also intentionally repeats
// literals and constructs entries in loops, while its orchestration has measured cognitive complexity.
@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals",
        "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.CouplingBetweenObjects",
        "PMD.TooManyMethods"
})
public class DemoTimetableSeeder {

    private static final String TIMETABLE_SEED_ACTION = "SEED_PLAN_081_FULL_V2";
    private static final String LEGACY_TIMETABLE_SEED_ACTION = "SEED_PLAN_081_G7";
    private static final List<String> TARGET_CLASS_CODES = List.of(
            "6A1", "6A2", "6A3", "6A4",
            "7A1", "7A2", "7A3", "7A4",
            "8A1", "8A2", "8A3", "8A4",
            "9A1", "9A2", "9A3", "9A4");
    private static final Map<String, List<SlotSeed>> SCHEDULES = DemoTimetableScheduleCatalog.schedules();
    private final DemoTimetableCatalogGateway catalogGateway;
    private final DemoTimetablePersistenceGateway persistenceGateway;
    private final DemoTimetableEntryPlanner entryPlanner;

    public DemoTimetableSeeder(
            DemoTimetableCatalogGateway catalogGateway,
            DemoTimetablePersistenceGateway persistenceGateway) {
        this.catalogGateway = catalogGateway;
        this.persistenceGateway = persistenceGateway;
        this.entryPlanner = new DemoTimetableEntryPlanner(catalogGateway, persistenceGateway);
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
        Map<String, Subject> subjectsByCode = catalogGateway.subjectRepository().findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(Subject::getCode, Function.identity()));
        Map<String, FunctionalRoom> roomsByCode = catalogGateway.roomRepository().findAll().stream()
                .filter(room -> Set.of("TIN-1", "TIN-2", "NGHE-1").contains(room.getCode()))
                .collect(Collectors.toMap(FunctionalRoom::getCode, Function.identity()));
        requireFixtureClasses(classesByCode);
        requireFixtureRooms(roomsByCode);
        requireFixtureMappings(subjectsByCode, roomsByCode);
        for (String semesterCode : List.of("HK1", "HK2")) {
            Semester semester = requireSemester(semestersByCode, semesterCode);
            TimetableHead head = persistenceGateway.headRepository().findBySemesterId(semester.getId()).orElse(null);
            if (hasSeededRevision(head, TIMETABLE_SEED_ACTION)
                    || (hasExistingEntries(head) && !canUpgradeLegacyFixture(head))) {
                continue;
            }
            TimetableCalendar calendar = ensureCalendar(semester.getId());
            Map<DemoTimetableEntryPlanner.PeriodKey, TimetablePeriod> periods = entryPlanner.ensurePeriods(
                    calendar.getId(), schedulesForSemester(semesterCode));
            TimetableRevision revision = createRevision(semester, head);
            List<DemoTimetableEntryPlanner.PlannedEntry> plannedEntries = entryPlanner.buildEntries(
                    semester, classesByCode, subjectsByCode, roomsByCode, periods);
            entryPlanner.assertNoConflicts(plannedEntries);
            head = persistenceGateway.headRepository().findBySemesterId(semester.getId())
                    .orElseThrow(() -> new IllegalStateException("Timetable head was not created"));
            persistenceGateway.entryRepository().saveAll(plannedEntries.stream()
                    .map(entry -> new TimetableEntry(
                            revision.getId(),
                            entry.assignmentId(),
                            entry.periodId(),
                            entry.functionalRoomId(),
                            semester.getStartDate(),
                            semester.getEndDate()))
                    .toList());
            persistenceGateway.auditRepository().save(new TimetableAudit(
                    head.getId(), revision.getId(), TIMETABLE_SEED_ACTION, null,
                    "seedKey=PLAN-081-FULL-V2;semester=" + semesterCode
                            + ";classes=6A1-9A4;subjectEntries=" + plannedEntries.size()
                            + ";dayOfWeek=1-5"));
            head.setCurrentRevisionId(revision.getId());
            persistenceGateway.headRepository().save(head);
        }
    }

    private void requireFixtureClasses(Map<String, SchoolClass> classesByCode) {
        if (classesByCode.size() != TARGET_CLASS_CODES.size()
                || !classesByCode.keySet().containsAll(TARGET_CLASS_CODES)) {
            throw new IllegalStateException("Plan 081 requires exactly the 16 classes 6A1-9A4");
        }
    }

    private void requireFixtureRooms(Map<String, FunctionalRoom> roomsByCode) {
        for (String roomCode : List.of("TIN-1", "TIN-2", "NGHE-1")) {
            if (roomsByCode.get(roomCode) == null) {
                throw new IllegalStateException("Missing Plan 081 functional room: " + roomCode);
            }
        }
    }

    private void requireFixtureMappings(
            Map<String, Subject> subjectsByCode,
            Map<String, FunctionalRoom> roomsByCode) {
        requireMapping(subjectsByCode, roomsByCode, "TIN_HOC", "TIN-1");
        requireMapping(subjectsByCode, roomsByCode, "TIN_HOC", "TIN-2");
        requireMapping(subjectsByCode, roomsByCode, "NGHE_DIEN", "NGHE-1");
        requireMapping(subjectsByCode, roomsByCode, "NGHE_NONG_NGHIEP", "NGHE-1");
    }

    private void requireMapping(
            Map<String, Subject> subjectsByCode,
            Map<String, FunctionalRoom> roomsByCode,
            String subjectCode,
            String roomCode) {
        Subject subject = subjectsByCode.get(subjectCode);
        FunctionalRoom room = roomsByCode.get(roomCode);
        if (subject == null || !catalogGateway.subjectFunctionalRoomRepository().existsBySubjectIdAndFunctionalRoomId(
                subject.getId(), room.getId())) {
            throw new IllegalStateException("Missing subject-functional-room mapping: "
                    + subjectCode + "/" + roomCode);
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

    private boolean hasSeededRevision(TimetableHead head, String action) {
        return head != null
                && persistenceGateway.revisionRepository().findByTimetableIdOrderByRevisionNumberDesc(head.getId()).stream()
                .anyMatch(revision -> persistenceGateway.auditRepository().existsByRevisionIdAndAction(
                        revision.getId(), action));
    }

    private boolean canUpgradeLegacyFixture(TimetableHead head) {
        if (head == null) {
            return false;
        }
        List<TimetableRevision> revisions = persistenceGateway.revisionRepository()
                .findByTimetableIdOrderByRevisionNumberDesc(head.getId());
        return revisions.stream()
                .filter(revision -> !persistenceGateway.entryRepository().findByRevisionId(revision.getId()).isEmpty())
                .allMatch(revision -> persistenceGateway.auditRepository().existsByRevisionIdAndAction(
                        revision.getId(), LEGACY_TIMETABLE_SEED_ACTION));
    }

    private boolean hasExistingEntries(TimetableHead head) {
        return head != null
                && persistenceGateway.revisionRepository().findByTimetableIdOrderByRevisionNumberDesc(head.getId()).stream()
                .anyMatch(revision -> !persistenceGateway.entryRepository().findByRevisionId(revision.getId()).isEmpty());
    }

    private TimetableCalendar ensureCalendar(Long semesterId) {
        return persistenceGateway.calendarRepository().findBySemesterId(semesterId)
                .orElseGet(() -> persistenceGateway.calendarRepository().save(new TimetableCalendar(semesterId)));
    }

    private TimetableRevision createRevision(Semester semester, TimetableHead existingHead) {
        TimetableHead head = existingHead != null
                ? existingHead
                : persistenceGateway.headRepository().save(new TimetableHead(semester.getId()));
        Integer maxRevision = persistenceGateway.revisionRepository().findMaxRevisionNumber(head.getId());
        return persistenceGateway.revisionRepository().save(new TimetableRevision(
                head.getId(),
                semester.getId(),
                maxRevision == null ? 1 : maxRevision + 1,
                semester.getStartDate(),
                semester.getEndDate(),
                null));
    }

    private List<SlotSeed> schedulesForSemester(String semesterCode) {
        return SCHEDULES.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(semesterCode + "|"))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

}
