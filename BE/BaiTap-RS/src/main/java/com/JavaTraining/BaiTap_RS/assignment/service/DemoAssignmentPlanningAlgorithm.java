package com.JavaTraining.BaiTap_RS.assignment.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DemoAssignmentPlanningAlgorithm {

    private static final Comparator<DemoAssignmentSeeder.WorkItem> WORK_ITEM_ORDER =
            Comparator.comparingInt(DemoAssignmentSeeder.WorkItem::periods).reversed()
                    .thenComparing(DemoAssignmentSeeder.WorkItem::subjectCode)
                    .thenComparing(DemoAssignmentSeeder.WorkItem::classCode);

    /* default */ List<DemoAssignmentSeeder.PlannedAssignment> plan(
            List<DemoAssignmentSeeder.WorkItem> workItems) {
        List<DemoAssignmentSeeder.PlannedAssignment> plan = new ArrayList<>();
        for (String semesterCode : List.of("HK1", "HK2")) {
            List<DemoAssignmentSeeder.WorkItem> semesterItems = workItems.stream()
                    .filter(item -> semesterCode.equals(item.semesterCode()))
                    .sorted(WORK_ITEM_ORDER)
                    .toList();
            Map<String, TeacherLoad> loads = createTeacherLoads();
            assignInitialPlan(semesterItems, plan, loads);
            repairMinimumLoads(plan, semesterCode, loads);
            validateLoads(semesterCode, loads);
        }
        return plan;
    }

    private void assignInitialPlan(
            List<DemoAssignmentSeeder.WorkItem> items,
            List<DemoAssignmentSeeder.PlannedAssignment> plan,
            Map<String, TeacherLoad> loads) {
        for (DemoAssignmentSeeder.WorkItem item : items) {
            String teacherCode = chooseTeacher(item, loads);
            loads.get(teacherCode).add(item.subjectCode(), item.periods());
            plan.add(new DemoAssignmentSeeder.PlannedAssignment(item, teacherCode));
        }
    }

    private Map<String, TeacherLoad> createTeacherLoads() {
        return DemoAssignmentFixture.EXPECTED_TEACHER_CODES.stream()
                .collect(java.util.stream.Collectors.toMap(
                        code -> code,
                        TeacherLoad::new,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private String chooseTeacher(
            DemoAssignmentSeeder.WorkItem item,
            Map<String, TeacherLoad> loads) {
        return DemoAssignmentFixture.EXPECTED_TEACHER_CODES.stream()
                .filter(teacherCode -> DemoAssignmentRules.teacherCanTeach(
                        teacherCode, item.subjectCode()))
                .filter(teacherCode -> loads.get(teacherCode).canAccept(
                        item.subjectCode(), item.periods()))
                .min(Comparator.comparingInt((String code) -> loads.get(code).weeklyLoad())
                        .thenComparingInt(code -> loads.get(code).subjectCount())
                        .thenComparing(code -> code))
                .orElseThrow(() -> new IllegalStateException("Cannot assign class " + item.classCode()
                        + ", semester " + item.semesterCode() + ", subject " + item.subjectCode()
                        + ": no A.5-eligible teacher has capacity "
                        + DemoAssignmentRules.maxWeeklyPeriods()));
    }

    private void repairMinimumLoads(
            List<DemoAssignmentSeeder.PlannedAssignment> plan,
            String semesterCode,
            Map<String, TeacherLoad> loads) {
        while (true) {
            String targetCode = loads.values().stream()
                    .filter(TeacherLoad::belowMinimum)
                    .min(Comparator.comparingInt(TeacherLoad::weeklyLoad)
                            .thenComparing(TeacherLoad::code))
                    .map(TeacherLoad::code)
                    .orElse(null);
            if (targetCode == null) {
                return;
            }
            Move move = findMove(plan, semesterCode, targetCode, loads);
            if (move == null) {
                TeacherLoad target = loads.get(targetCode);
                throw new IllegalStateException("Cannot satisfy minimum weekly load for " + targetCode
                        + " in " + semesterCode + ": current " + target.weeklyLoad()
                        + ", required " + target.minimum());
            }
            applyMove(plan, move, targetCode, loads);
        }
    }

    private Move findMove(
            List<DemoAssignmentSeeder.PlannedAssignment> plan,
            String semesterCode,
            String targetCode,
            Map<String, TeacherLoad> loads) {
        TeacherLoad target = loads.get(targetCode);
        int bestIndex = -1;
        String bestSourceCode = null;
        int bestPeriods = 0;
        for (int index = 0; index < plan.size(); index++) {
            DemoAssignmentSeeder.PlannedAssignment assignment = plan.get(index);
            DemoAssignmentSeeder.WorkItem item = assignment.item();
            TeacherLoad source = loads.get(assignment.teacherCode());
            if (!canMove(item, assignment, semesterCode, targetCode, target, source)) {
                continue;
            }
            if (bestIndex < 0 || isBetterMove(index, assignment.teacherCode(), item.periods(),
                    bestIndex, bestSourceCode, bestPeriods, loads)) {
                bestIndex = index;
                bestSourceCode = assignment.teacherCode();
                bestPeriods = item.periods();
            }
        }
        return bestIndex < 0 ? null : new Move(bestIndex, bestSourceCode, bestPeriods);
    }

    private boolean canMove(
            DemoAssignmentSeeder.WorkItem item,
            DemoAssignmentSeeder.PlannedAssignment assignment,
            String semesterCode,
            String targetCode,
            TeacherLoad target,
            TeacherLoad source) {
        return semesterCode.equals(item.semesterCode())
                && !targetCode.equals(assignment.teacherCode())
                && DemoAssignmentRules.teacherCanTeach(targetCode, item.subjectCode())
                && target.canAccept(item.subjectCode(), item.periods())
                && source.weeklyLoad() - item.periods() >= source.minimum();
    }

    private boolean isBetterMove(
            int candidateIndex,
            String candidateSourceCode,
            int candidatePeriods,
            int currentIndex,
            String currentSourceCode,
            int currentPeriods,
            Map<String, TeacherLoad> loads) {
        int candidateLoad = loads.get(candidateSourceCode).weeklyLoad();
        int currentLoad = loads.get(currentSourceCode).weeklyLoad();
        return candidatePeriods > currentPeriods
                || candidatePeriods == currentPeriods && candidateLoad > currentLoad
                || candidatePeriods == currentPeriods && candidateLoad == currentLoad
                && candidateIndex < currentIndex;
    }

    private void applyMove(
            List<DemoAssignmentSeeder.PlannedAssignment> plan,
            Move move,
            String targetCode,
            Map<String, TeacherLoad> loads) {
        DemoAssignmentSeeder.PlannedAssignment current = plan.get(move.index());
        String subjectCode = current.item().subjectCode();
        loads.get(move.sourceCode()).remove(subjectCode, move.periods());
        loads.get(targetCode).add(subjectCode, move.periods());
        plan.set(move.index(), new DemoAssignmentSeeder.PlannedAssignment(current.item(), targetCode));
    }

    private void validateLoads(String semesterCode, Map<String, TeacherLoad> loads) {
        for (TeacherLoad load : loads.values()) {
            if (load.weeklyLoad() < load.minimum()
                    || load.weeklyLoad() > DemoAssignmentRules.maxWeeklyPeriods()) {
                throw new IllegalStateException("Invalid weekly load for " + load.code()
                        + " in " + semesterCode + ": " + load.weeklyLoad()
                        + " not in [" + load.minimum() + ", "
                        + DemoAssignmentRules.maxWeeklyPeriods() + "]");
            }
            if (load.subjectCount() > 2) {
                throw new IllegalStateException("Teacher " + load.code()
                        + " exceeds 2 subjects in " + semesterCode);
            }
        }
    }

    private record Move(int index, String sourceCode, int periods) {
    }

    private static final class TeacherLoad {

        private final String teacherIdentifier;
        private final Map<String, Integer> periodsBySubject = new LinkedHashMap<>();
        private int loadPeriods;

        private TeacherLoad(String code) {
            this.teacherIdentifier = code;
        }

        private void add(String subjectCode, int additionalPeriods) {
            loadPeriods += additionalPeriods;
            if (additionalPeriods > 0) {
                periodsBySubject.merge(subjectCode, additionalPeriods, Integer::sum);
            }
        }

        private void remove(String subjectCode, int removedPeriods) {
            loadPeriods -= removedPeriods;
            int remaining = periodsBySubject.getOrDefault(subjectCode, 0) - removedPeriods;
            if (remaining > 0) {
                periodsBySubject.put(subjectCode, remaining);
            } else {
                periodsBySubject.remove(subjectCode);
            }
        }

        private boolean canAccept(String subjectCode, int additionalPeriods) {
            return loadPeriods + additionalPeriods <= DemoAssignmentRules.maxWeeklyPeriods()
                    && (periodsBySubject.containsKey(subjectCode) || subjectCount() < 2);
        }

        private boolean belowMinimum() {
            return loadPeriods < minimum();
        }

        private int minimum() {
            return DemoAssignmentRules.minimumWeeklyPeriods(teacherIdentifier);
        }

        private int subjectCount() {
            return periodsBySubject.size();
        }

        private String code() {
            return teacherIdentifier;
        }

        private int weeklyLoad() {
            return loadPeriods;
        }
    }
}
