package com.JavaTraining.BaiTap_RS.bootstrap;

import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.service.DemoAcademicApplicabilitySeeder;
import com.JavaTraining.BaiTap_RS.academic.service.DemoAcademicCatalogSeeder;
import com.JavaTraining.BaiTap_RS.assignment.service.DemoAssignmentSeeder;
import com.JavaTraining.BaiTap_RS.identity.DemoIdentitySeeder;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final DemoIdentitySeeder identitySeeder;
    private final DemoAcademicCatalogSeeder catalogSeeder;
    private final DemoAcademicApplicabilitySeeder applicabilitySeeder;
    private final DemoAssignmentSeeder assignmentSeeder;

    public DemoDataSeeder(
            DemoIdentitySeeder identitySeeder,
            DemoAcademicCatalogSeeder catalogSeeder,
            DemoAcademicApplicabilitySeeder applicabilitySeeder,
            DemoAssignmentSeeder assignmentSeeder) {
        this.identitySeeder = identitySeeder;
        this.catalogSeeder = catalogSeeder;
        this.applicabilitySeeder = applicabilitySeeder;
        this.assignmentSeeder = assignmentSeeder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User academicOffice = identitySeeder.seedAcademicOffice();
        List<Teacher> teachers = identitySeeder.seedTeachers();
        AcademicYear academicYear = catalogSeeder.seedAcademicYear();
        List<Semester> semesters = catalogSeeder.seedSemesters(academicYear);
        Map<Integer, GradeLevel> grades = catalogSeeder.seedGrades();
        List<SchoolClass> classes = catalogSeeder.seedClasses(academicYear, grades);
        Map<String, Subject> subjects = catalogSeeder.seedSubjects();
        applicabilitySeeder.seedApplicability(subjects, semesters, grades);
        List<Student> students = identitySeeder.seedStudents(classes);
        identitySeeder.seedEnrollments(students, classes, academicYear);
        List<ClassSubject> classSubjects = applicabilitySeeder.seedClassSubjects(classes, semesters, subjects);
        assignmentSeeder.seed(classes, semesters, classSubjects, teachers, academicOffice.getId());
    }
}
