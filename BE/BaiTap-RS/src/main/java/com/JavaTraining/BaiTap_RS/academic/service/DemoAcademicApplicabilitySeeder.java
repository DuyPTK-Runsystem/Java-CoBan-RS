package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import org.springframework.stereotype.Component;

@Component
public class DemoAcademicApplicabilitySeeder {

    private final DemoAcademicApplicabilityStore applicabilityStore;

    public DemoAcademicApplicabilitySeeder(
            DemoAcademicApplicabilityStore applicabilityStore) {
        this.applicabilityStore = applicabilityStore;
    }

    public void seedApplicability(
            Map<String, Subject> subjects,
            List<Semester> semesters,
            Map<Integer, GradeLevel> grades) {
        List<SubjectApplicability> existing = applicabilityStore.loadExistingApplicability();
        for (Subject subject : subjects.values()) {
            seedSubjectApplicability(subject, semesters, grades, existing);
        }
    }

    public List<ClassSubject> seedClassSubjects(
            List<SchoolClass> classes,
            List<Semester> semesters,
            Map<String, Subject> subjects) {
        List<ClassSubject> classSubjects = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            seedClassSubjectsForClass(schoolClass, semesters, subjects, classSubjects);
        }
        return classSubjects;
    }

    private void seedSubjectApplicability(
            Subject subject,
            List<Semester> semesters,
            Map<Integer, GradeLevel> grades,
            List<SubjectApplicability> existing) {
        for (Semester semester : semesters) {
            for (Map.Entry<Integer, GradeLevel> grade : grades.entrySet()) {
                if (DemoAcademicApplicabilityRules.isApplicable(
                        subject, grade.getKey(), semester.getCode())) {
                    applicabilityStore.saveApplicability(subject, semester, grade.getValue(), existing);
                }
            }
        }
    }

    private void seedClassSubjectsForClass(
            SchoolClass schoolClass,
            List<Semester> semesters,
            Map<String, Subject> subjects,
            List<ClassSubject> classSubjects) {
        int grade = Integer.parseInt(schoolClass.getClassCode().substring(0, 1));
        for (Semester semester : semesters) {
            for (Subject subject : subjects.values()) {
                if (DemoAcademicApplicabilityRules.isApplicableForClass(
                        subject, schoolClass, grade, semester.getCode())) {
                    classSubjects.add(applicabilityStore.findOrCreateClassSubject(
                            schoolClass, semester, subject));
                }
            }
        }
    }

}
