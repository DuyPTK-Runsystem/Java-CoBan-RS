package com.JavaTraining.BaiTap_RS.identity;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentGender;
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
@SuppressWarnings({
        "PMD.TooManyMethods",
        "PMD.AvoidDuplicateLiterals",
        "PMD.UseVarargs"
})
public class DemoIdentitySeeder {

    private static final String ACADEMIC_OFFICE_USERNAME = "academic.office";
    private static final String DEMO_PASSWORD = "12345678";
    private static final String[][] CANONICAL_STUDENTS = buildCanonicalStudents();

    private static String[][] buildCanonicalStudents() {
        List<String[]> students = new ArrayList<>();
        addStudents(students, new String[][]{
                {"Nguyễn Minh Anh", "FEMALE"}, {"Nguyễn Gia Bảo", "MALE"},
                {"Nguyễn Đức Anh", "MALE"}, {"Nguyễn Hải Anh", "MALE"},
                {"Nguyễn Tuấn Anh", "MALE"}, {"Nguyễn Nhật Anh", "MALE"},
                {"Nguyễn Hoài An", "FEMALE"}, {"Nguyễn Bình An", "MALE"},
                {"Nguyễn Khánh An", "FEMALE"}, {"Nguyễn Gia An", "FEMALE"},
                {"Nguyễn Minh An", "FEMALE"}, {"Nguyễn Đức An", "MALE"},
                {"Nguyễn Hải An", "MALE"}, {"Nguyễn Phúc An", "MALE"},
                {"Nguyễn Quốc An", "MALE"}, {"Nguyễn Gia Huy", "MALE"},
                {"Nguyễn Minh Huy", "MALE"}, {"Nguyễn Đức Huy", "MALE"},
                {"Nguyễn Hải Huy", "MALE"},
                {"Nguyễn Quốc Huy", "MALE"}, {"Nguyễn Tuấn Huy", "MALE"},
                {"Nguyễn Nhật Huy", "MALE"}, {"Nguyễn Khánh Huy", "MALE"},
                {"Nguyễn Phúc Huy", "MALE"}, {"Nguyễn Anh Huy", "MALE"},
                {"Nguyễn Gia Khang", "MALE"}, {"Nguyễn Minh Khang", "MALE"},
                {"Nguyễn Đức Khang", "MALE"}, {"Nguyễn Hải Khang", "MALE"},
                {"Nguyễn Hữu Khang", "MALE"}, {"Nguyễn Gia Khôi", "MALE"},
                {"Nguyễn Minh Khôi", "MALE"}, {"Nguyễn Đức Khôi", "MALE"},
                {"Nguyễn Hải Khôi", "MALE"}, {"Nguyễn Anh Khôi", "MALE"},
                {"Nguyễn Gia Long", "MALE"}, {"Nguyễn Minh Long", "MALE"},
                {"Nguyễn Đức Long", "MALE"}, {"Nguyễn Hải Long", "MALE"},
                {"Nguyễn Thành Long", "MALE"}, {"Nguyễn Gia Nam", "MALE"},
                {"Nguyễn Minh Nam", "MALE"}, {"Nguyễn Đức Nam", "MALE"},
                {"Nguyễn Hải Nam", "MALE"}, {"Nguyễn Hoài Nam", "MALE"},
                {"Nguyễn Quốc Nam", "MALE"}, {"Nguyễn Tuấn Nam", "MALE"},
                {"Nguyễn Nhật Nam", "MALE"}, {"Nguyễn Khánh Nam", "MALE"},
                {"Nguyễn Phúc Nam", "MALE"}, {"Nguyễn Gia Phúc", "MALE"},
                {"Nguyễn Minh Phúc", "MALE"}, {"Nguyễn Đức Phúc", "MALE"},
                {"Nguyễn Hải Phúc", "MALE"}, {"Nguyễn Anh Phúc", "MALE"},
                {"Nguyễn Gia Sơn", "MALE"}, {"Nguyễn Minh Sơn", "MALE"},
                {"Nguyễn Đức Sơn", "MALE"}, {"Nguyễn Hải Sơn", "MALE"},
                {"Nguyễn Hoàng Sơn", "MALE"}, {"Nguyễn Gia Tâm", "MALE"},
                {"Nguyễn Minh Tâm", "MALE"}, {"Nguyễn Đức Tâm", "MALE"},
                {"Nguyễn Hải Tâm", "MALE"}, {"Nguyễn Hoài Tâm", "MALE"},
                {"Nguyễn Gia Tú", "MALE"}, {"Nguyễn Minh Tú", "MALE"},
                {"Nguyễn Đức Tú", "MALE"}, {"Nguyễn Hải Tú", "MALE"},
                {"Nguyễn Anh Tú", "MALE"}, {"Nguyễn Gia Vy", "FEMALE"},
                {"Nguyễn Minh Vy", "FEMALE"}, {"Nguyễn Thảo Vy", "FEMALE"},
                {"Nguyễn Khánh Vy", "FEMALE"}, {"Nguyễn Bảo Vy", "FEMALE"},
                {"Nguyễn Gia Linh", "FEMALE"}, {"Nguyễn Minh Linh", "FEMALE"},
                {"Nguyễn Thảo Linh", "FEMALE"}, {"Nguyễn Khánh Linh", "FEMALE"},
                {"Nguyễn Bảo Linh", "FEMALE"}, {"Nguyễn Gia Mai", "FEMALE"},
                {"Nguyễn Minh Mai", "FEMALE"}, {"Nguyễn Thảo Mai", "FEMALE"},
                {"Nguyễn Khánh Mai", "FEMALE"}, {"Nguyễn Bảo Mai", "FEMALE"},
                {"Nguyễn Gia My", "FEMALE"}, {"Nguyễn Minh My", "FEMALE"},
                {"Nguyễn Thảo My", "FEMALE"}, {"Nguyễn Khánh My", "FEMALE"},
                {"Nguyễn Bảo My", "FEMALE"}, {"Nguyễn Gia Nhi", "FEMALE"},
                {"Nguyễn Minh Nhi", "FEMALE"}, {"Nguyễn Thảo Nhi", "FEMALE"},
                {"Nguyễn Khánh Nhi", "FEMALE"}, {"Nguyễn Bảo Nhi", "FEMALE"},
                {"Nguyễn Gia Yến", "FEMALE"}, {"Nguyễn Minh Yến", "FEMALE"},
                {"Nguyễn Thảo Yến", "FEMALE"}, {"Nguyễn Khánh Yến", "FEMALE"},
                {"Nguyễn Bảo Yến", "FEMALE"}
        });
        addStudents(students, new String[][]{
                {"Lý Minh Anh", "FEMALE"}, {"Lý Gia Bảo", "MALE"},
                {"Lý Đức Anh", "MALE"}, {"Lý Hải Anh", "MALE"},
                {"Lý Tuấn Anh", "MALE"}, {"Lý Nhật Anh", "MALE"},
                {"Lý Hoài An", "FEMALE"}, {"Lý Bình An", "MALE"},
                {"Lý Khánh An", "FEMALE"}, {"Lý Gia An", "FEMALE"},
                {"Lý Minh An", "FEMALE"}, {"Lý Đức An", "MALE"},
                {"Lý Hải An", "MALE"}, {"Lý Phúc An", "MALE"},
                {"Lý Quốc An", "MALE"}, {"Lý Gia Huy", "MALE"},
                {"Lý Minh Huy", "MALE"}, {"Lý Đức Huy", "MALE"},
                {"Lý Hải Huy", "MALE"}, {"Lý Quốc Huy", "MALE"},
                {"Lý Tuấn Huy", "MALE"}, {"Lý Nhật Huy", "MALE"},
                {"Lý Khánh Huy", "MALE"}, {"Lý Phúc Huy", "MALE"},
                {"Lý Anh Huy", "MALE"}, {"Lý Gia Khang", "MALE"},
                {"Lý Minh Khang", "MALE"}, {"Lý Đức Khang", "MALE"},
                {"Lý Hải Khang", "MALE"}, {"Lý Hữu Khang", "MALE"},
                {"Lý Gia Khôi", "MALE"}, {"Lý Minh Khôi", "MALE"},
                {"Lý Đức Khôi", "MALE"}, {"Lý Hải Khôi", "MALE"},
                {"Lý Anh Khôi", "MALE"}, {"Lý Gia Long", "MALE"},
                {"Lý Minh Long", "MALE"}, {"Lý Đức Long", "MALE"},
                {"Lý Hải Long", "MALE"}, {"Lý Thành Long", "MALE"},
                {"Lý Gia Nam", "MALE"}, {"Lý Minh Nam", "MALE"},
                {"Lý Đức Nam", "MALE"}, {"Lý Hải Nam", "MALE"},
                {"Lý Hoài Nam", "MALE"}, {"Lý Quốc Nam", "MALE"},
                {"Lý Tuấn Nam", "MALE"}, {"Lý Nhật Nam", "MALE"},
                {"Lý Khánh Nam", "MALE"}, {"Lý Phúc Nam", "MALE"},
                {"Lý Gia Linh", "FEMALE"}, {"Lý Minh Linh", "FEMALE"},
                {"Lý Thảo Linh", "FEMALE"}, {"Lý Khánh Linh", "FEMALE"},
                {"Lý Bảo Linh", "FEMALE"}, {"Lý Gia Nhi", "FEMALE"},
                {"Lý Minh Nhi", "FEMALE"}, {"Lý Thảo Nhi", "FEMALE"},
                {"Lý Khánh Nhi", "FEMALE"}, {"Lý Bảo Nhi", "FEMALE"}
        });
        if (students.size() != 160) {
            throw new IllegalStateException("Demo student catalog must contain 160 students");
        }
        return students.toArray(new String[0][]);
    }

    private static void addStudents(List<String[]> target, String[][] names) {
        for (String[] name : names) {
            target.add(new String[]{studentUsername(name[0]), name[0], name[1]});
        }
    }

    private static String studentUsername(String fullName) {
        String normalized = Normalizer.normalize(fullName, Normalizer.Form.NFD)
                .replace("đ", "d").replace("Đ", "D")
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace(' ', '.');
        String[] words = normalized.split("\\.");
        String username = normalized;
        if (username.length() > 17) {
            int middleLength = 17 - words[0].length() - words[words.length - 1].length() - 2;
            username = words[0] + "." + words[1].substring(0, middleLength)
                    + "." + words[words.length - 1];
        }
        return username;
    }
    private static final String[][] CANONICAL_TEACHERS = {
            {"GV001", "pham.minh.quan", "Phạm Minh Quân", "MALE", "0900000001"},
            {"GV002", "tran.thu.ha", "Trần Thu Hà", "FEMALE", "0900000002"},
            {"GV003", "le.hoang.nam", "Lê Hoàng Nam", "MALE", "0900000003"},
            {"GV004", "pham.ngoc.lan", "Phạm Ngọc Lan", "FEMALE", "0900000004"},
            {"GV005", "tran.quoc.bao", "Trần Quốc Bảo", "MALE", "0900000005"},
            {"GV006", "le.thi.huong", "Lê Thị Hương", "FEMALE", "0900000006"},
            {"GV007", "pham.duc.anh", "Phạm Đức Anh", "MALE", "0900000007"},
            {"GV008", "tran.mai.phuong", "Trần Mai Phương", "FEMALE", "0900000008"},
            {"GV009", "le.van.thanh", "Lê Văn Thành", "MALE", "0900000009"},
            {"GV010", "pham.thuy.dung", "Phạm Thùy Dung", "FEMALE", "0900000010"},
            {"GV011", "tran.minh.khoi", "Trần Minh Khôi", "MALE", "0900000011"},
            {"GV012", "le.ngoc.mai", "Lê Ngọc Mai", "FEMALE", "0900000012"},
            {"GV013", "pham.huu.dat", "Phạm Hữu Đạt", "MALE", "0900000013"},
            {"GV014", "tran.thanh.van", "Trần Thanh Vân", "FEMALE", "0900000014"},
            {"GV015", "le.cong.thanh", "Lê Công Thành", "MALE", "0900000015"},
            {"GV016", "pham.khanh.linh", "Phạm Khánh Linh", "FEMALE", "0900000016"},
            {"GV017", "tran.anh.dung", "Trần Anh Dũng", "MALE", "0900000017"},
            {"GV018", "le.thu.trang", "Lê Thu Trang", "FEMALE", "0900000018"},
            {"GV019", "pham.tuan.kiet", "Phạm Tuấn Kiệt", "MALE", "0900000019"},
            {"GV020", "tran.hai.yen", "Trần Hải Yến", "FEMALE", "0900000020"}
    };

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
        for (int index = 0; index < CANONICAL_TEACHERS.length; index++) {
            String[] fixture = CANONICAL_TEACHERS[index];
            String code = fixture[0];
            String username = fixture[1];
            Teacher teacher = teachers.stream()
                    .filter(existing -> code.equals(existing.getTeacherCode()))
                    .findFirst()
                    .orElse(null);
            User user = resolveTeacherUser(teacher, username);
            if (teacher == null) {
                teacher = createTeacher(index + 1, fixture, user.getId());
                teachers.add(teacher);
            } else {
                updateTeacher(teacher, fixture);
            }
            teachers.set(teachers.indexOf(teacher), teacherRepository.save(teacher));
        }
        teachers.sort(Comparator.comparing(Teacher::getTeacherCode));
        return teachers;
    }

    public List<Student> seedStudents(List<SchoolClass> classes) {
        List<Student> students = new ArrayList<>();
        List<SchoolClass> orderedClasses = new ArrayList<>(classes);
        orderedClasses.sort(Comparator.comparing(SchoolClass::getClassCode));
        int sequence = 1;
        for (SchoolClass schoolClass : orderedClasses) {
            int grade = Integer.parseInt(schoolClass.getClassCode().substring(0, 1));
            for (int number = 1; number <= 10; number++) {
                String code = String.format("STU260%04d", sequence++);
                Student student = studentRepository.findByStudentCode(code).orElse(null);
                String username = CANONICAL_STUDENTS[number - 1][0]
                        + compactClassSuffix(schoolClass.getClassCode());
                User user = resolveStudentUser(student, username);
                if (student == null) {
                    student = createStudent(schoolClass, grade, number, code, user.getId());
                } else if (!Objects.equals(student.getUserId(), user.getId())) {
                    throw new IllegalStateException("Student code is linked to another user: " + code);
                }
                student.setStatus(StudentStatus.ACTIVE);
                ensureStudentInfo(student, schoolClass, grade, number);
                updateStudent(student, schoolClass, grade, number);
                students.add(studentRepository.save(student));
            }
        }
        return students;
    }

    private String compactClassSuffix(String classCode) {
        String normalized = classCode.toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1) + normalized.substring(2);
    }

    public void seedEnrollments(
            List<Student> students,
            List<SchoolClass> classes,
            AcademicYear academicYear) {
        LocalDateTime enrolledAt = LocalDateTime.of(2026, 9, 1, 8, 0);
        List<SchoolClass> orderedClasses = new ArrayList<>(classes);
        orderedClasses.sort(Comparator.comparing(SchoolClass::getClassCode));
        for (int index = 0; index < students.size(); index++) {
            Student student = students.get(index);
            if (isTargetedGradeSevenStudent(student)) {
                continue;
            }
            SchoolClass schoolClass = orderedClasses.get(index / 10);
            if (enrollmentRepository.findByStudentIdAndAcademicYearId(
                    student.getId(), academicYear.getId()).isEmpty()) {
                enrollmentRepository.save(createEnrollment(
                        student, schoolClass, academicYear, enrolledAt));
            }
        }
    }

    private boolean isTargetedGradeSevenStudent(Student student) {
        return "STU2600041".compareTo(student.getStudentCode()) <= 0
                && "STU2600080".compareTo(student.getStudentCode()) >= 0;
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

    private User resolveTeacherUser(Teacher teacher, String canonicalUsername) {
        if (teacher != null && teacher.getUserId() != null) {
            return userRepository.findById(teacher.getUserId())
                    .orElseGet(() -> ensureUser(canonicalUsername, DEMO_PASSWORD, "TEACHER"));
        }
        return ensureUser(canonicalUsername, DEMO_PASSWORD, "TEACHER");
    }

    private User resolveStudentUser(Student student, String canonicalUsername) {
        if (student != null && student.getUserId() != null) {
            return userRepository.findById(student.getUserId())
                    .map(user -> ensureRole(user, "STUDENT"))
                    .orElseGet(() -> ensureUser(canonicalUsername, DEMO_PASSWORD, "STUDENT"));
        }
        return ensureUser(canonicalUsername, DEMO_PASSWORD, "STUDENT");
    }

    private User ensureRole(User user, String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Missing seeded role: " + roleCode));
        if (user.getRoles().stream().noneMatch(existing -> roleCode.equals(existing.getCode()))) {
            user.addRole(role);
        }
        return userRepository.save(user);
    }

    private Teacher createTeacher(int index, String[] fixture, Long userId) {
        return new Teacher(
                userId,
                fixture[0],
                fixture[2],
                LocalDate.of(1980 + index % 10, 1 + index % 12, 1 + index % 20),
                fixture[3],
                fixture[4],
                fixture[1] + "@example.test",
                "Tổ bộ môn",
                LocalDate.of(2010 + index % 10, 8, 15),
                TeacherStatus.ACTIVE);
    }

    private void updateTeacher(Teacher teacher, String[] fixture) {
        teacher.setTeacherCode(fixture[0]);
        teacher.setTeacherName(fixture[2]);
        teacher.setGender(fixture[3]);
        teacher.setPhone(fixture[4]);
        teacher.setEmail(fixture[1] + "@example.test");
        teacher.setStatus(TeacherStatus.ACTIVE);
    }

    private Student createStudent(
            SchoolClass schoolClass,
            int grade,
            int number,
            String code,
            Long userId) {
        Student student = new Student(
                CANONICAL_STUDENTS[number - 1][1],
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

    private void updateStudent(Student student, SchoolClass schoolClass, int grade, int number) {
        String[] fixture = CANONICAL_STUDENTS[number - 1];
        student.setStudentName(fixture[1]);
        StudentInfo info = student.getStudentInfo();
        info.setDateOfBirth(LocalDate.of(2010 + 9 - grade, 5, 10 + number));
        info.setAddress("Khu phố " + schoolClass.getClassCode());
        info.setGender(StudentGender.valueOf(fixture[2]));
    }

    private StudentInfo createStudentInfo(SchoolClass schoolClass, int grade, int number) {
        return new StudentInfo(
                LocalDate.of(2010 + 9 - grade, 5, 10 + number),
                "Khu phố " + schoolClass.getClassCode(),
                null,
                StudentGender.valueOf(CANONICAL_STUDENTS[number - 1][2]));
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
