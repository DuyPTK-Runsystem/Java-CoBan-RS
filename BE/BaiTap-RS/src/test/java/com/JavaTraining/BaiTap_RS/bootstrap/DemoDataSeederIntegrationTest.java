package com.JavaTraining.BaiTap_RS.bootstrap;

import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-seeder;MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER,ROLE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "app.seed.demo.enabled=true"
})
class DemoDataSeederIntegrationTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String DEFAULT_PASSWORD = "12345678";

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentYearEnrollmentRepository enrollmentRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private HomeroomAssignmentRepository homeroomAssignmentRepository;

    @Autowired
    private SubjectTeachingAssignmentRepository teachingAssignmentRepository;

    @Autowired
    private SubjectApplicabilityRepository applicabilityRepository;

    @Autowired
    private ClassSubjectRepository classSubjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsDeterministicIdentityAcademicAndAssignmentFixture() {
        demoDataSeeder.run(new DefaultApplicationArguments());

        assertTrue(userRepository.count() == 54
                && teacherRepository.count() == 20
                && studentRepository.count() == 32
                && enrollmentRepository.count() == 32
                && schoolClassRepository.count() == 8
                && enrollmentRepository.findAll().stream()
                .collect(Collectors.groupingBy(StudentYearEnrollment::getCurrentClassId, Collectors.counting()))
                .values().stream().allMatch(count -> count == 4)
                && homeroomAssignmentRepository.count() == 8
                && applicabilityRepository.count() == 91
                && classSubjectRepository.count() == 182
                && teachingAssignmentRepository.count() == 182
                && userRepository.findByUsername(ADMIN_USERNAME)
                .filter(user -> passwordEncoder.matches(ADMIN_PASSWORD, user.getPassword()))
                .isPresent()
                && userRepository.findByUsername("academic.office")
                .filter(user -> passwordEncoder.matches(DEFAULT_PASSWORD, user.getPassword())
                        && user.getRoles().stream().anyMatch(role -> "ACADEMIC_OFFICE".equals(role.getCode())))
                .isPresent()
                && userRepository.findByUsername("student.6a1.01")
                .filter(user -> passwordEncoder.matches(DEFAULT_PASSWORD, user.getPassword())
                        && user.getRoles().stream().anyMatch(role -> "STUDENT".equals(role.getCode())))
                .isPresent()
                && userRepository.findAll().stream().allMatch(user -> {
                    String expectedPassword = ADMIN_USERNAME.equals(user.getUsername())
                            ? ADMIN_PASSWORD : DEFAULT_PASSWORD;
                    return passwordEncoder.matches(expectedPassword, user.getPassword());
                })
                && teacherRepository.findAll().stream().allMatch(teacher -> teacher.getUserId() != null
                        && userRepository.findById(teacher.getUserId())
                        .filter(user -> user.getRoles().stream()
                                .anyMatch(role -> "TEACHER".equals(role.getCode())))
                        .isPresent())
                && studentRepository.findAll().stream()
                .allMatch(student -> student.getUserId() != null && student.getStudentInfo() != null),
                "demo fixture counts, credentials, and identity links must be deterministic");
    }
}
