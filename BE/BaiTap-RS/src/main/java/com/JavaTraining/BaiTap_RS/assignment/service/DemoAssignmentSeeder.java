package com.JavaTraining.BaiTap_RS.assignment.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
public class DemoAssignmentSeeder {

    private final DemoAssignmentCatalog catalog;
    private final DemoAssignmentPlanner assignmentPlanner;
    private final DemoAssignmentPersistence persistence;

    public DemoAssignmentSeeder(
            DemoAssignmentCatalog catalog,
            DemoAssignmentPlanner assignmentPlanner,
            DemoAssignmentPersistence persistence) {
        this.catalog = catalog;
        this.assignmentPlanner = assignmentPlanner;
        this.persistence = persistence;
    }

    public void seed(
            List<SchoolClass> classes,
            List<Semester> semesters,
            List<ClassSubject> classSubjects,
            List<Teacher> teachers,
            Long assignedBy) {
        DemoAssignmentCatalog.SeedData data = catalog.prepare(
                classes, semesters, classSubjects, teachers);
        persistence.seedHomerooms(data.classesByCode(), data.teachersByCode(), assignedBy);
        List<PlannedAssignment> plan = assignmentPlanner.plan(data.workItems());
        persistence.persistAssignments(plan, data.teachersByCode(), data.semestersByCode(), assignedBy);
    }

    /* default */ record WorkItem(
            ClassSubject classSubject,
            String classCode,
            String subjectCode,
            String semesterCode,
            int periods) {
    }

    /* default */ record PlannedAssignment(WorkItem item, String teacherCode) {
    }
}
