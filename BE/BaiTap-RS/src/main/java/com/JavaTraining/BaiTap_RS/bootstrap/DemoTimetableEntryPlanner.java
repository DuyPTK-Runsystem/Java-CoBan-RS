package com.JavaTraining.BaiTap_RS.bootstrap;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
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
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.bootstrap.DemoTimetableScheduleCatalog.SlotSeed;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;

@SuppressWarnings({
        "PMD.CognitiveComplexity",
        "PMD.CouplingBetweenObjects"
})
final class DemoTimetableEntryPlanner {

    private static final Map<String, List<SlotSeed>> SCHEDULES = DemoTimetableScheduleCatalog.schedules();
    private static final List<String> TARGET_CLASS_CODES = List.of(
            "6A1", "6A2", "6A3", "6A4",
            "7A1", "7A2", "7A3", "7A4",
            "8A1", "8A2", "8A3", "8A4",
            "9A1", "9A2", "9A3", "9A4");
    private final DemoTimetableCatalogGateway catalogGateway;
    private final DemoTimetablePersistenceGateway persistenceGateway;

    /* package */
    DemoTimetableEntryPlanner(
            DemoTimetableCatalogGateway catalogGateway,
            DemoTimetablePersistenceGateway persistenceGateway) {
        this.catalogGateway = catalogGateway;
        this.persistenceGateway = persistenceGateway;
    }

    /* package */
    Map<PeriodKey, TimetablePeriod> ensurePeriods(Long calendarId, List<SlotSeed> schedule) {
        Map<PeriodKey, TimetablePeriod> periods = persistenceGateway.periodRepository()
                .findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(calendarId).stream()
                .collect(Collectors.toMap(
                        period -> new PeriodKey(period.getDayOfWeek(), period.getSession(), period.getPeriodIndex()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        schedule.stream()
                .map(slot -> new PeriodKey(slot.dayOfWeek(), slot.session(), slot.periodIndex()))
                .distinct()
                .forEach(key -> periods.computeIfAbsent(key, periodKey -> persistenceGateway.periodRepository().save(
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

    /* package */
    List<PlannedEntry> buildEntries(
            Semester semester,
            Map<String, SchoolClass> classesByCode,
            Map<String, Subject> subjectsByCode,
            Map<String, FunctionalRoom> roomsByCode,
            Map<PeriodKey, TimetablePeriod> periods) {
        List<PlannedEntry> entries = new ArrayList<>();
        Set<String> occupiedRooms = new HashSet<>();
        TARGET_CLASS_CODES.forEach(classCode -> {
            SchoolClass schoolClass = classesByCode.get(classCode);
            Map<Long, ClassSubject> classSubjectsBySubjectId = catalogGateway.classSubjectRepository()
                    .findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(schoolClass.getId(), semester.getId()).stream()
                    .filter(classSubject -> classSubject.getStatus() == ClassSubjectStatus.ACTIVE)
                    .collect(Collectors.toMap(ClassSubject::getSubjectId, Function.identity()));
            List<SlotSeed> classSchedule = SCHEDULES.get(scheduleKey(semester.getCode(), classCode));
            if (classSchedule == null) {
                throw new IllegalStateException("Missing schedule for " + semester.getCode() + "/" + classCode);
            }
            for (SlotSeed slot : classSchedule) {
                Subject subject = subjectsByCode.get(slot.subjectCode());
                if (subject == null) {
                    throw new IllegalStateException("Missing subject: " + slot.subjectCode());
                }
                ClassSubject classSubject = classSubjectsBySubjectId.get(subject.getId());
                if (classSubject == null) {
                    throw new IllegalStateException("Missing active class_subject for "
                            + classCode + "/" + semester.getCode() + "/" + slot.subjectCode());
                }
                SubjectTeachingAssignment assignment = catalogGateway.assignmentRepository()
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
                Long roomId = resolveFunctionalRoom(slot.subjectCode(), period.getId(),
                        occupiedRooms, roomsByCode);
                entries.add(new PlannedEntry(
                        schoolClass.getId(),
                        assignment.getId(),
                        assignment.getTeacherId(),
                        period.getId(),
                        roomId));
            }
        });
        return entries;
    }

    /* package */
    void assertNoConflicts(List<PlannedEntry> entries) {
        Set<String> classSlots = new HashSet<>();
        Set<String> teacherSlots = new HashSet<>();
        Set<String> roomSlots = new HashSet<>();
        for (PlannedEntry entry : entries) {
            requireUnique(classSlots, entry.classId() + "|" + entry.periodId(), "class");
            requireUnique(teacherSlots, entry.teacherId() + "|" + entry.periodId(), "teacher");
            if (entry.functionalRoomId() != null) {
                requireUnique(roomSlots, entry.functionalRoomId() + "|" + entry.periodId(), "room");
            }
        }
    }

    private Long resolveFunctionalRoom(
            String subjectCode,
            Long periodId,
            Set<String> occupiedRooms,
            Map<String, FunctionalRoom> roomsByCode) {
        if ("TIN_HOC".equals(subjectCode)) {
            for (String roomCode : List.of("TIN-1", "TIN-2")) {
                if (occupiedRooms.add(roomCode + "|" + periodId)) {
                    return roomsByCode.get(roomCode).getId();
                }
            }
            throw new IllegalStateException("Two Tin rooms are insufficient at period " + periodId);
        }
        if ("NGHE_DIEN".equals(subjectCode) || "NGHE_NONG_NGHIEP".equals(subjectCode)) {
            if (!occupiedRooms.add("NGHE-1|" + periodId)) {
                throw new IllegalStateException("NGHE-1 conflict at period " + periodId);
            }
            return roomsByCode.get("NGHE-1").getId();
        }
        return null;
    }

    private void requireUnique(Set<String> occupied, String key, String resource) {
        if (!occupied.add(key)) {
            throw new IllegalStateException("Plan 081 timetable " + resource + " conflict at " + key);
        }
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

    /* package */
    record PeriodKey(int dayOfWeek, SessionType session, int periodIndex) {
    }

    /* package */
    record PlannedEntry(Long classId, Long assignmentId, Long teacherId, Long periodId,
            Long functionalRoomId) {
    }
}
