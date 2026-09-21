package com.JavaTraining.BaiTap_RS.assignment.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
final class DemoAssignmentCatalog {

    private static final List<String> SEMESTER_CODES = List.of("HK1", "HK2");
    private final SubjectRepository subjectRepository;

    /* default */ DemoAssignmentCatalog(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    /* default */ SeedData prepare(
            List<SchoolClass> classes,
            List<Semester> semesters,
            List<ClassSubject> classSubjects,
            List<Teacher> teachers) {
        Map<String, SchoolClass> classesByCode = DemoAssignmentFixture.indexByCode(
                classes, SchoolClass::getClassCode, "class");
        Map<String, Teacher> teachersByCode = DemoAssignmentFixture.indexByCode(
                teachers, Teacher::getTeacherCode, "teacher");
        Map<String, Semester> semestersByCode = indexSemesters(semesters);
        Map<String, Long> subjectIdsByCode = DemoAssignmentFixture.loadSubjectIds(subjectRepository);
        DemoAssignmentFixture.validateRequiredReferences(
                classesByCode.keySet(), teachersByCode.keySet(), subjectIdsByCode.keySet());
        List<DemoAssignmentSeeder.WorkItem> workItems = buildWorkItems(
                classSubjects, classesByCode, semestersByCode, subjectIdsByCode);
        return new SeedData(classesByCode, teachersByCode, semestersByCode, workItems);
    }

    private List<DemoAssignmentSeeder.WorkItem> buildWorkItems(
            List<ClassSubject> classSubjects,
            Map<String, SchoolClass> classesByCode,
            Map<String, Semester> semestersByCode,
            Map<String, Long> subjectIdsByCode) {
        if (classSubjects == null) {
            throw new IllegalStateException("Missing class_subject data for all four grades");
        }
        Map<Long, String> subjectCodesById = subjectIdsByCode.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Map<String, ClassSubject> activeByKey = indexActiveClassSubjects(
                classSubjects, classesByCode, semestersByCode, subjectCodesById);
        List<DemoAssignmentSeeder.WorkItem> workItems = new ArrayList<>();
        List<String> sortedSubjectCodes = subjectIdsByCode.keySet().stream().sorted().toList();
        for (String classCode : DemoAssignmentFixture.EXPECTED_CLASS_CODES) {
            addClassWorkItems(classCode, semestersByCode, sortedSubjectCodes, activeByKey, workItems);
        }
        return workItems;
    }

    private void addClassWorkItems(
            String classCode,
            Map<String, Semester> semestersByCode,
            List<String> sortedSubjectCodes,
            Map<String, ClassSubject> activeByKey,
            List<DemoAssignmentSeeder.WorkItem> workItems) {
        int grade = Integer.parseInt(classCode.substring(0, 1));
        for (String semesterCode : SEMESTER_CODES) {
            if (!semestersByCode.containsKey(semesterCode)) {
                throw new IllegalStateException("Missing required semester: " + semesterCode);
            }
            for (String subjectCode : sortedSubjectCodes) {
                int periods = DemoAssignmentRules.weeklyPeriods(subjectCode, grade, semesterCode);
                boolean selected = !DemoAssignmentRules.isSkill(subjectCode)
                        || DemoAssignmentRules.skillForClass(classCode).equals(subjectCode);
                if (periods > 0 && selected) {
                    addWorkItem(classCode, semesterCode, subjectCode, periods, activeByKey, workItems);
                }
            }
        }
    }

    private void addWorkItem(
            String classCode,
            String semesterCode,
            String subjectCode,
            int periods,
            Map<String, ClassSubject> activeByKey,
            List<DemoAssignmentSeeder.WorkItem> workItems) {
        String key = classSubjectKey(classCode, semesterCode, subjectCode);
        ClassSubject classSubject = activeByKey.get(key);
        if (classSubject == null) {
            throw new IllegalStateException("Missing active class_subject for class " + classCode
                    + ", semester " + semesterCode + ", subject " + subjectCode);
        }
        workItems.add(new DemoAssignmentSeeder.WorkItem(
                classSubject, classCode, subjectCode, semesterCode, periods));
    }

    private Map<String, ClassSubject> indexActiveClassSubjects(
            List<ClassSubject> classSubjects,
            Map<String, SchoolClass> classesByCode,
            Map<String, Semester> semestersByCode,
            Map<Long, String> subjectCodesById) {
        Map<Long, String> classCodesById = classesByCode.values().stream()
                .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getClassCode));
        Map<Long, String> semesterCodesById = semestersByCode.values().stream()
                .collect(Collectors.toMap(Semester::getId, Semester::getCode));
        Map<String, ClassSubject> activeByKey = new LinkedHashMap<>();
        for (ClassSubject classSubject : classSubjects) {
            if (classSubject.getStatus() == ClassSubjectStatus.ACTIVE) {
                indexActiveClassSubject(classSubject, classCodesById, semesterCodesById,
                        subjectCodesById, activeByKey);
            }
        }
        return activeByKey;
    }

    private void indexActiveClassSubject(
            ClassSubject classSubject,
            Map<Long, String> classCodesById,
            Map<Long, String> semesterCodesById,
            Map<Long, String> subjectCodesById,
            Map<String, ClassSubject> activeByKey) {
        String classCode = classCodesById.get(classSubject.getClassId());
        String semesterCode = semesterCodesById.get(classSubject.getSemesterId());
        String subjectCode = subjectCodesById.get(classSubject.getSubjectId());
        if (classCode == null || semesterCode == null || subjectCode == null) {
            throw new IllegalStateException("class_subject references missing catalog data: id "
                    + classSubject.getId());
        }
        String key = classSubjectKey(classCode, semesterCode, subjectCode);
        if (activeByKey.put(key, classSubject) != null) {
            throw new IllegalStateException("Duplicate active class_subject for " + key);
        }
    }

    private Map<String, Semester> indexSemesters(List<Semester> semesters) {
        if (semesters == null) {
            throw new IllegalStateException("Missing semester data for all four grades");
        }
        Map<String, Semester> result = semesters.stream()
                .filter(semester -> SEMESTER_CODES.contains(semester.getCode()))
                .collect(Collectors.toMap(
                        Semester::getCode,
                        semester -> semester,
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate required semester: " + left.getCode());
                        },
                        LinkedHashMap::new));
        SEMESTER_CODES.stream()
                .filter(code -> !result.containsKey(code))
                .findFirst()
                .ifPresent(code -> {
                    throw new IllegalStateException("Missing required semester: " + code);
                });
        return result;
    }

    private String classSubjectKey(String classCode, String semesterCode, String subjectCode) {
        return classCode + "|" + semesterCode + "|" + subjectCode;
    }

    /* default */ record SeedData(
            Map<String, SchoolClass> classesByCode,
            Map<String, Teacher> teachersByCode,
            Map<String, Semester> semestersByCode,
            List<DemoAssignmentSeeder.WorkItem> workItems) {
    }
}
