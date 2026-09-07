package com.JavaTraining.BaiTap_RS.identity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.RoleRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoIdentitySeeder {

    private static final String ACADEMIC_OFFICE_USERNAME = "academic.office";
    private static final String DEMO_PASSWORD = "12345678";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoIdentitySeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User seedAcademicOffice() {
        return ensureUser(ACADEMIC_OFFICE_USERNAME, DEMO_PASSWORD, "ACADEMIC_OFFICE");
    }

    public List<Teacher> seedTeachers() {
        List<Teacher> teachers = new ArrayList<>(teacherRepository.findAllByOrderByTeacherCodeAsc());
        for (int index = 1; index <= 20; index++) {
            String code = String.format("GV%03d", index);
            String username = String.format("teacher%02d", index);
            User user = ensureUser(username, DEMO_PASSWORD, "TEACHER");
            Teacher teacher = teachers.stream()
                    .filter(existing -> code.equals(existing.getTeacherCode()))
                    .findFirst()
                    .orElse(null);
            if (teacher != null && !Objects.equals(teacher.getUserId(), user.getId())) {
                throw new IllegalStateException("Teacher code is linked to another user: " + code);
            }
            if (teacher == null) {
                teacher = createTeacher(index, code, username, user.getId());
                teachers.add(teacher);
            }
            teachers.set(teachers.indexOf(teacher), teacherRepository.save(teacher));
        }
        teachers.sort(java.util.Comparator.comparing(Teacher::getTeacherCode));
        return teachers;
    }

    public List<Student> seedStudents(List<SchoolClass> classes) {
        List<Student> students = new ArrayList<>();
        int sequence = 1;
        for (SchoolClass schoolClass : classes) {
            int grade = Integer.parseInt(schoolClass.getClassCode().substring(0, 1));
            for (int number = 1; number <= 4; number++) {
                String code = String.format("STU260%04d", sequence++);
                String username = String.format(
                        "student.%s.%02d", schoolClass.getClassCode().toLowerCase(Locale.ROOT), number);
                User user = ensureUser(username, DEMO_PASSWORD, "STUDENT");
                Student student = studentRepository.findByStudentCode(code).orElse(null);
                if (student == null) {
                    student = createStudent(schoolClass, grade, number, code, user.getId());
                } else if (!Objects.equals(student.getUserId(), user.getId())) {
                    throw new IllegalStateException("Student code is linked to another user: " + code);
                }
                student.setStatus(StudentStatus.ACTIVE);
                ensureStudentInfo(student, schoolClass, grade, number);
                students.add(studentRepository.save(student));
            }
        }
        return students;
    }

    public void seedEnrollments(
            List<Student> students,
            List<SchoolClass> classes,
            AcademicYear academicYear) {
        LocalDateTime enrolledAt = LocalDateTime.of(2026, 9, 1, 8, 0);
        for (int index = 0; index < students.size(); index++) {
            Student student = students.get(index);
            SchoolClass schoolClass = classes.get(index / 4);
            if (enrollmentRepository.findByStudentIdAndAcademicYearId(
                    student.getId(), academicYear.getId()).isEmpty()) {
                enrollmentRepository.save(createEnrollment(student, schoolClass, academicYear, enrolledAt));
            }
        }
    }

    private User ensureUser(String username, String rawPassword, String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Missing seeded role: " + roleCode));
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> new User(username, passwordEncoder.encode(rawPassword)));
        if (user.getRoles().stream().noneMatch(existing -> roleCode.equals(existing.getCode()))) {
            user.addRole(role);
        }
        return userRepository.save(user);
    }

    private Teacher createTeacher(int index, String code, String username, Long userId) {
        return new Teacher(
                userId,
                code,
                "Giáo viên " + String.format("%02d", index),
                LocalDate.of(1980 + index % 10, 1 + index % 12, 1 + index % 20),
                index % 2 == 0 ? "FEMALE" : "MALE",
                "090000" + String.format("%04d", index),
                username + "@example.test",
                "Tổ bộ môn",
                LocalDate.of(2010 + index % 10, 8, 15),
                TeacherStatus.ACTIVE);
    }

    private Student createStudent(
            SchoolClass schoolClass,
            int grade,
            int number,
            String code,
            Long userId) {
        Student student = new Student(
                "Học sinh " + schoolClass.getClassCode() + " " + String.format("%02d", number),
                code);
        student.setUserId(userId);
        student.assignInfo(createStudentInfo(schoolClass, grade, number));
        return student;
    }

    private void ensureStudentInfo(Student student, SchoolClass schoolClass, int grade, int number) {
        if (student.getStudentInfo() == null) {
            student.assignInfo(createStudentInfo(schoolClass, grade, number));
        }
    }

    private StudentInfo createStudentInfo(SchoolClass schoolClass, int grade, int number) {
        return new StudentInfo(
                LocalDate.of(2010 + 9 - grade, 5, 10 + number),
                "Khu phố " + schoolClass.getClassCode(),
                null);
    }

    private StudentYearEnrollment createEnrollment(
            Student student,
            SchoolClass schoolClass,
            AcademicYear academicYear,
            LocalDateTime enrolledAt) {
        return new StudentYearEnrollment(
                student.getId(),
                academicYear.getId(),
                schoolClass.getId(),
                EnrollmentStatus.ACTIVE,
                enrolledAt);
    }
}
