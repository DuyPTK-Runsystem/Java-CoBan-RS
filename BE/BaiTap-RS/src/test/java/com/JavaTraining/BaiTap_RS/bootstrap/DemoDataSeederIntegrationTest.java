package com.JavaTraining.BaiTap_RS.bootstrap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResultStatus;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSessionStatus;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementCandidateRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementResultRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementSessionRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationResultSource;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableAudit;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private GradeLevelRepository gradeLevelRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private HomeroomAssignmentRepository homeroomAssignmentRepository;

    @Autowired
    private SubjectTeachingAssignmentRepository teachingAssignmentRepository;

    @Autowired
    private SubjectApplicabilityRepository applicabilityRepository;

    @Autowired
    private ClassSubjectRepository classSubjectRepository;

    @Autowired
    private DemoPlacementSeeder placementSeeder;

    @Autowired
    private DemoNotificationSeeder notificationSeeder;

    @Autowired
    private DemoFunctionalRoomSeeder functionalRoomSeeder;

    @Autowired
    private DemoScorebookSeeder scorebookSeeder;

    @Autowired
    private PlacementSessionRepository placementSessionRepository;

    @Autowired
    private PlacementCandidateRepository placementCandidateRepository;

    @Autowired
    private PlacementResultRepository placementResultRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationReceiptRepository notificationReceiptRepository;

    @Autowired
    private FunctionalRoomRepository functionalRoomRepository;

    @Autowired
    private SubjectFunctionalRoomRepository subjectFunctionalRoomRepository;

    @Autowired
    private ScorebookRepository scorebookRepository;

    @Autowired
    private AssessmentColumnRepository assessmentColumnRepository;

    @Autowired
    private StudentScoreRepository studentScoreRepository;

    @Autowired
    private StudentAnnualTranscriptRepository annualTranscriptRepository;

    @Autowired
    private StudentTermTranscriptRepository termTranscriptRepository;

    @Autowired
    private StudentSubjectTermResultRepository termResultRepository;

    @Autowired
    private StudentSubjectAnnualResultRepository annualResultRepository;

    @Autowired
    private TimetableHeadRepository timetableHeadRepository;

    @Autowired
    private TimetableRevisionRepository timetableRevisionRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private TimetableAuditRepository timetableAuditRepository;

    @Autowired
    private TimetableCalendarRepository timetableCalendarRepository;

    @Autowired
    private TimetablePeriodRepository timetablePeriodRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsDeterministicIdentityAcademicAndAssignmentFixture() {
        runAllDemoSeeders();

        AcademicYear currentAcademicYear = academicYearRepository.findAll().stream()
                .filter(year -> "2026-2027".equals(year.getCode()))
                .findFirst()
                .orElseThrow();
        List<StudentYearEnrollment> currentEnrollments = enrollmentRepository.findAll().stream()
                .filter(enrollment -> currentAcademicYear.getId().equals(enrollment.getAcademicYearId()))
                .toList();
        Set<Long> currentClassIds = schoolClassRepository.findAll().stream()
                .filter(schoolClass -> currentAcademicYear.getId().equals(schoolClass.getAcademicYearId()))
                .map(schoolClass -> schoolClass.getId())
                .collect(Collectors.toSet());
        Set<Long> currentSemesterIds = semesterRepository
                .findAllByAcademicYearIdOrderByDisplayOrderAsc(currentAcademicYear.getId()).stream()
                .map(semester -> semester.getId())
                .collect(Collectors.toSet());
        List<ClassSubject> currentClassSubjects = classSubjectRepository.findAll().stream()
                .filter(classSubject -> currentClassIds.contains(classSubject.getClassId()))
                .toList();
        assertTrue(userRepository.count() == 182
                && teacherRepository.count() == 20
                && studentRepository.count() == 160
                && enrollmentRepository.count() == 200
                && schoolClassRepository.count() == 20
                && currentEnrollments.size() == 120
                && currentEnrollments.stream()
                .collect(Collectors.groupingBy(StudentYearEnrollment::getCurrentClassId, Collectors.counting()))
                .size() == 12
                && currentEnrollments.stream()
                .collect(Collectors.groupingBy(StudentYearEnrollment::getCurrentClassId, Collectors.counting()))
                .values().stream().allMatch(count -> count == 10)
                && schoolClassRepository.findAll().stream().allMatch(schoolClass -> schoolClass.getCapacity() == 10)
                && homeroomAssignmentRepository.count() == 16
                && applicabilityRepository.findAll().stream()
                .filter(applicability -> currentSemesterIds.contains(applicability.getSemesterId()))
                .filter(applicability -> "ACTIVE".equals(applicability.getStatus().name()))
                .count() == 107
                && currentClassSubjects.size() == 340
                && teachingAssignmentRepository.count() == 340
                && userRepository.findByUsername(ADMIN_USERNAME)
                .filter(user -> passwordEncoder.matches(ADMIN_PASSWORD, user.getPassword()))
                .isPresent()
                && userRepository.findByUsername("academic.office")
                .filter(user -> passwordEncoder.matches(DEFAULT_PASSWORD, user.getPassword())
                        && user.getRoles().stream().anyMatch(role -> "ACADEMIC_OFFICE".equals(role.getCode())))
                .isPresent()
                && userRepository.findByUsername("nguyen.minh.anh61")
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

        List<String> studentNames = studentRepository.findAll().stream()
                .map(Student::getStudentName)
                .toList();
        assertEquals(160, studentNames.stream().distinct().count(),
                "all demo student names must be distinct");
        assertTrue(studentNames.contains("Lý Minh Anh"));
        assertTrue(studentNames.contains("Lý Bảo Nhi"));

        Map<String, TeacherFixture> expectedTeachers = canonicalTeachers();
        Map<String, TeacherFixture> actualTeachers = teacherRepository.findAll().stream()
                .collect(Collectors.toMap(
                        teacher -> teacher.getTeacherCode(),
                        teacher -> {
                            String username = userRepository.findById(teacher.getUserId())
                                    .orElseThrow().getUsername();
                            return new TeacherFixture(username, teacher.getTeacherName());
                        }));
        assertEquals(expectedTeachers, actualTeachers,
                "all seeded teachers must use the Plan 081 canonical code, username, and name mapping");
        assertTrue(teacherRepository.findAll().stream().allMatch(teacher -> {
            TeacherFixture fixture = expectedTeachers.get(teacher.getTeacherCode());
            return fixture != null
                    && fixture.username().equals(
                            userRepository.findById(teacher.getUserId()).orElseThrow().getUsername())
                    && (fixture.username() + "@example.test").equals(teacher.getEmail());
        }), "canonical teacher emails must be deterministic");
    }

    @Test
    void seedsFullAcademicScopeAndTeachingAssignmentsForBothSemesters() {
        runAllDemoSeeders();

        AcademicYear currentAcademicYear = academicYearRepository.findAll().stream()
                .filter(year -> "2026-2027".equals(year.getCode()))
                .findFirst()
                .orElseThrow();
        Set<Long> currentClassIds = schoolClassRepository.findAll().stream()
                .filter(schoolClass -> currentAcademicYear.getId().equals(schoolClass.getAcademicYearId()))
                .map(schoolClass -> schoolClass.getId())
                .collect(Collectors.toSet());
        Set<Long> currentSemesterIds = semesterRepository
                .findAllByAcademicYearIdOrderByDisplayOrderAsc(currentAcademicYear.getId()).stream()
                .map(semester -> semester.getId())
                .collect(Collectors.toSet());
        Map<Long, String> classCodesById = schoolClassRepository.findAll().stream()
                .filter(schoolClass -> currentClassIds.contains(schoolClass.getId()))
                .collect(Collectors.toMap(
                        schoolClass -> schoolClass.getId(), schoolClass -> schoolClass.getClassCode()));
        Map<Long, String> teacherCodesById = teacherRepository.findAll().stream()
                .collect(Collectors.toMap(teacher -> teacher.getId(), teacher -> teacher.getTeacherCode()));
        Map<Long, String> subjectCodesById = subjectRepository.findAll().stream()
                .collect(Collectors.toMap(subject -> subject.getId(), subject -> subject.getCode()));
        Map<Long, String> semesterCodesById = semesterRepository.findAll().stream()
                .collect(Collectors.toMap(semester -> semester.getId(), semester -> semester.getCode()));
        Map<Long, ClassSubject> classSubjectsById = classSubjectRepository.findAll().stream()
                .filter(classSubject -> currentClassIds.contains(classSubject.getClassId()))
                .collect(Collectors.toMap(classSubject -> classSubject.getId(), classSubject -> classSubject));

        Map<String, String> expectedHomerooms = canonicalHomerooms();
        assertExactSet(expectedHomerooms.keySet(), Set.copyOf(classCodesById.values()), "class fixture");
        assertExactSet(canonicalTeachers().keySet(), Set.copyOf(teacherCodesById.values()), "teacher fixture");

        Map<String, String> actualHomerooms = homeroomAssignmentRepository.findAll().stream()
                .collect(Collectors.toMap(
                        assignment -> resolveCode(classCodesById, assignment.getClassId(), "class"),
                        assignment -> resolveCode(teacherCodesById, assignment.getTeacherId(), "teacher")));
        assertEquals(expectedHomerooms, actualHomerooms,
                "active homeroom fixture must map all 16 classes to GV001..GV016");

        Map<Long, Integer> gradeById = gradeLevelRepository.findAll().stream()
                .collect(Collectors.toMap(grade -> grade.getId(), grade -> grade.getLevel()));
        Set<String> applicabilityKeys = applicabilityRepository.findAll().stream()
                .filter(applicability -> currentSemesterIds.contains(applicability.getSemesterId()))
                .filter(applicability -> "ACTIVE".equals(applicability.getStatus().name()))
                .map(applicability -> applicabilityKey(
                        applicability,
                        subjectCodesById,
                        semesterCodesById,
                        gradeById))
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                        "HOA_HOC|8|HK1", "HOA_HOC|8|HK2",
                        "HOA_HOC|9|HK1", "HOA_HOC|9|HK2"),
                applicabilityKeys.stream()
                        .filter(key -> key.startsWith("HOA_HOC|"))
                        .collect(Collectors.toSet()),
                "HOA_HOC must be applicable only to grades 8 and 9 in both semesters");
        assertTrue(applicabilityKeys.contains("CONG_NGHE|9|HK1"),
                "CONG_NGHE must remain applicable to grade 9 in HK1");
        assertFalse(applicabilityKeys.contains("CONG_NGHE|9|HK2"),
                "CONG_NGHE must not be applicable to grade 9 in HK2");
        assertEquals(Set.of("CONG_NGHE|6|HK2", "CONG_NGHE|7|HK2", "CONG_NGHE|8|HK2"),
                applicabilityKeys.stream()
                        .filter(key -> key.startsWith("CONG_NGHE|") && key.endsWith("|HK2"))
                        .collect(Collectors.toSet()),
                "CONG_NGHE HK2 scope must be grades 6-8 only");

        Map<String, Long> classSubjectCountsBySemester = classSubjectRepository.findAll().stream()
                .filter(classSubject -> currentClassIds.contains(classSubject.getClassId()))
                .collect(Collectors.groupingBy(
                        classSubject -> semesterCodesById.get(classSubject.getSemesterId()),
                        Collectors.counting()));
        assertEquals(Map.of("HK1", 168L, "HK2", 172L), classSubjectCountsBySemester,
                "class_subject counts must follow the corrected subject scope");
        assertEquals(340L, classSubjectsById.size(),
                "full academic year must contain 340 class_subject rows");

        Set<String> expectedSkillClasses = Set.of("8A1", "8A2", "8A3", "8A4",
                "9A1", "9A2", "9A3", "9A4");
        Map<String, Set<String>> skillsByClass = classSubjectRepository.findAll().stream()
                .filter(classSubject -> currentClassIds.contains(classSubject.getClassId()))
                .filter(classSubject -> "HK2".equals(semesterCodesById.get(classSubject.getSemesterId())))
                .filter(classSubject -> expectedSkillClasses.contains(classCodesById.get(classSubject.getClassId())))
                .filter(classSubject -> Set.of("NGHE_DIEN", "NGHE_NONG_NGHIEP")
                        .contains(subjectCodesById.get(classSubject.getSubjectId())))
                .collect(Collectors.groupingBy(
                        classSubject -> classCodesById.get(classSubject.getClassId()),
                        Collectors.mapping(classSubject -> subjectCodesById.get(classSubject.getSubjectId()),
                                Collectors.toSet())));
        assertEquals(expectedSkillClasses, skillsByClass.keySet(),
                "every grade 8/9 class must have one HK2 skill subject");
        assertTrue(skillsByClass.values().stream().allMatch(skills -> skills.size() == 1),
                "each grade 8/9 HK2 class must have exactly one skill, never both");

        List<SubjectTeachingAssignment> assignments = teachingAssignmentRepository.findAll();
        assertEquals(340, assignments.size(), "every active class_subject must have one teaching assignment");
        assertTrue(assignments.stream().allMatch(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE),
                "all full-year teaching assignments must be active");
        Map<Long, Long> assignedTeacherByClassSubject = assignments.stream()
                .collect(Collectors.toMap(
                        SubjectTeachingAssignment::getClassSubjectId,
                        SubjectTeachingAssignment::getTeacherId));
        assertEquals(classSubjectsById.keySet(), assignedTeacherByClassSubject.keySet(),
                "teaching assignments must cover every class_subject exactly once");

        Map<String, Set<String>> actualSubjectsByTeacher = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> resolveCode(teacherCodesById, assignment.getTeacherId(), "teacher"),
                        Collectors.mapping(assignment -> resolveCode(subjectCodesById,
                                classSubjectsById.get(assignment.getClassSubjectId()).getSubjectId(), "subject"),
                                Collectors.toSet())));
        assertEquals(canonicalTeachers().keySet(), actualSubjectsByTeacher.keySet(),
                "no teacher may be idle when the full academic year is assigned");
        assertTrue(actualSubjectsByTeacher.values().stream().allMatch(subjects -> subjects.size() <= 2),
                "each teacher must teach at most 2 unique subjects");

        Map<String, Map<String, Integer>> weeklyLoadByTeacher = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> resolveCode(teacherCodesById, assignment.getTeacherId(), "teacher"),
                        Collectors.groupingBy(
                                assignment -> semesterCodesById.get(
                                        classSubjectsById.get(assignment.getClassSubjectId()).getSemesterId()),
                                Collectors.summingInt(assignment -> weeklyPeriods(
                                        assignment,
                                        classSubjectsById,
                                        classCodesById,
                                        subjectCodesById,
                                        semesterCodesById)))));
        assertEquals(Map.of("HK1", 376, "HK2", 388),
                weeklyLoadByTeacher.values().stream()
                        .flatMap(loads -> loads.entrySet().stream())
                        .collect(Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.summingInt(Map.Entry::getValue))),
                "weekly totals must follow the corrected grade-specific period rules");
        assertTrue(weeklyLoadByTeacher.entrySet().stream().allMatch(entry -> {
            String teacherCode = entry.getKey();
            int minimum = expectedMinimumLoad(teacherCode, expectedHomerooms);
            return Set.of("HK1", "HK2").stream().allMatch(semester -> {
                int load = entry.getValue().getOrDefault(semester, 0);
                return load >= minimum && load <= 24;
            });
        }), "each teacher must be within the role-based [15/19, 24] weekly bounds per semester");

        long homerooms = homeroomAssignmentRepository.count();
        long teachingAssignments = teachingAssignmentRepository.count();
        Map<Long, Long> assignmentOwners = Map.copyOf(assignedTeacherByClassSubject);
        runAllDemoSeeders();
        assertEquals(homerooms, homeroomAssignmentRepository.count(), "homeroom seed must be idempotent");
        assertEquals(teachingAssignments, teachingAssignmentRepository.count(),
                "full teaching assignment seed must be idempotent");
        assertEquals(assignmentOwners,
                teachingAssignmentRepository.findAll().stream().collect(Collectors.toMap(
                        SubjectTeachingAssignment::getClassSubjectId,
                        SubjectTeachingAssignment::getTeacherId)),
                "idempotent reseeding must preserve class_subject teacher ownership");
    }

    @Test
    void seedsUnassignedGradeSevenAndFullSixteenClassTimetableWithIdempotency() {
        runAllDemoSeeders();

        AcademicYear currentAcademicYear = academicYearRepository.findAll().stream()
                .filter(year -> "2026-2027".equals(year.getCode()))
                .findFirst()
                .orElseThrow();
        Map<String, Long> classIdsByCode = schoolClassRepository.findAll().stream()
                .filter(schoolClass -> currentAcademicYear.getId().equals(schoolClass.getAcademicYearId()))
                .collect(Collectors.toMap(
                        schoolClass -> schoolClass.getClassCode(),
                        schoolClass -> schoolClass.getId()));
        Map<Long, ClassSubject> classSubjectsById = classSubjectRepository.findAll().stream()
                .filter(classSubject -> classIdsByCode.containsValue(classSubject.getClassId()))
                .collect(Collectors.toMap(ClassSubject::getId, classSubject -> classSubject));
        Map<Long, SubjectTeachingAssignment> assignmentsById = teachingAssignmentRepository.findAll().stream()
                .collect(Collectors.toMap(SubjectTeachingAssignment::getId, assignment -> assignment));
        Map<Long, String> subjectCodesById = subjectRepository.findAll().stream()
                .collect(Collectors.toMap(subject -> subject.getId(), subject -> subject.getCode()));
        Map<Long, String> semesterCodesById = semesterRepository.findAll().stream()
                .collect(Collectors.toMap(semester -> semester.getId(), semester -> semester.getCode()));

        assertEquals(expectedFixtureClassCodes(), classIdsByCode.keySet(),
                "the timetable fixture must contain exactly the 16 Plan 081 classes");
        assertUnassignedGradeSeven();

        TimetableSnapshot hk1BeforeRerun = assertFullTimetable(
                classIdsByCode, classSubjectsById, assignmentsById, subjectCodesById, semesterCodesById,
                "HK1", 376, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        TimetableSnapshot hk2BeforeRerun = assertFullTimetable(
                classIdsByCode, classSubjectsById, assignmentsById, subjectCodesById, semesterCodesById,
                "HK2", 388, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 5, 31));

        runAllDemoSeeders();
        assertTimetableUnchanged(hk1BeforeRerun, "HK1 rerun");
        assertTimetableUnchanged(hk2BeforeRerun, "HK2 rerun");
    }

    private void assertUnassignedGradeSeven() {
        AcademicYear academicYear = academicYearRepository.findAll().stream()
                .filter(year -> "2026-2027".equals(year.getCode()))
                .findFirst()
                .orElseThrow();
        Set<String> expectedCodes = new HashSet<>();
        for (int sequence = 41; sequence <= 80; sequence++) {
            expectedCodes.add(String.format("STU260%04d", sequence));
        }
        Set<String> actualG7Codes = studentRepository.findAllByStudentCodeIn(expectedCodes).stream()
                .filter(student -> "ACTIVE".equals(student.getStatus().name()))
                .map(student -> student.getStudentCode())
                .collect(Collectors.toSet());
        assertEquals(expectedCodes, actualG7Codes, "G7 demo students must be exactly 40 ACTIVE students");
        assertEquals(120, enrollmentRepository.findByAcademicYearIdAndStatusOrderByStudentIdAsc(
                academicYear.getId(), EnrollmentStatus.ACTIVE).size(),
                "only the 120 non-G7 students may have active enrollment");
        assertTrue(studentRepository.findAllByStudentCodeIn(expectedCodes).stream()
                .allMatch(student -> enrollmentRepository.findByStudentIdAndAcademicYearId(
                        student.getId(), academicYear.getId()).isEmpty()),
                "G7 demo students must have no enrollment in the target academic year");
        assertEquals(expectedCodes,
                enrollmentRepository.findUnassignedStudents(academicYear.getId()).stream()
                        .map(student -> student.getStudentCode()).collect(Collectors.toSet()),
                "the unassigned-student query must return exactly the 40 G7 demo students");
    }

    private TimetableSnapshot assertFullTimetable(
            Map<String, Long> classIdsByCode,
            Map<Long, ClassSubject> classSubjectsById,
            Map<Long, SubjectTeachingAssignment> assignmentsById,
            Map<Long, String> subjectCodesById,
            Map<Long, String> semesterCodesById,
            String semesterCode,
            int expectedEntryCount,
            LocalDate expectedStart,
            LocalDate expectedEnd) {
        var semester = semesterRepository.findAll().stream()
                .filter(candidate -> semesterCode.equals(candidate.getCode()))
                .findFirst()
                .orElseThrow();
        var head = timetableHeadRepository.findBySemesterId(semester.getId()).orElseThrow();
        List<TimetableRevision> revisions = timetableRevisionRepository
                .findByTimetableIdOrderByRevisionNumberDesc(head.getId());
        List<TimetableRevision> seededRevisions = revisions.stream()
                .filter(revision -> timetableAuditRepository.existsByRevisionIdAndAction(
                        revision.getId(), "SEED_PLAN_081_FULL_V2"))
                .toList();
        assertEquals(1, seededRevisions.size(), semesterCode + " must have one full seeded revision");

        TimetableRevision revision = seededRevisions.get(0);
        assertEquals(TimetableRevisionStatus.DRAFT, revision.getStatus(), semesterCode + " must remain DRAFT");
        assertEquals(semester.getId(), revision.getSemesterId());
        assertEquals(expectedStart, revision.getEffectiveFrom());
        assertEquals(expectedEnd, revision.getEffectiveTo());
        assertEquals(0, revision.getBlockingCount());
        assertEquals(revision.getId(), head.getCurrentRevisionId());

        List<TimetableEntry> entries = timetableEntryRepository.findByRevisionId(revision.getId());
        assertEquals(expectedEntryCount, entries.size(), semesterCode + " subject entry count");
        Map<Long, TimetablePeriod> periodsById = timetablePeriodRepository
                .findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(
                        timetableCalendarRepository.findBySemesterId(semester.getId()).orElseThrow().getId())
                .stream()
                .collect(Collectors.toMap(TimetablePeriod::getId, period -> period));
        Set<String> classSlots = new HashSet<>();
        Set<String> teacherSlots = new HashSet<>();
        Set<String> roomSlots = new HashSet<>();
        Map<Long, String> classCodesById = classIdsByCode.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getValue(), entry -> entry.getKey()));
        for (TimetableEntry entry : entries) {
            assertEquals(expectedStart, entry.getValidFrom());
            assertEquals(expectedEnd, entry.getValidTo());
            TimetablePeriod period = periodsById.get(entry.getPeriodId());
            assertTrue(period != null && period.getDayOfWeek() >= 1 && period.getDayOfWeek() <= 5,
                    semesterCode + " entries must use Monday-Friday periods");
            SubjectTeachingAssignment assignment = assignmentsById.get(entry.getAssignmentId());
            assertTrue(assignment != null && assignment.getStatus() == AssignmentStatus.ACTIVE,
                    semesterCode + " entries must use active assignments");
            ClassSubject classSubject = classSubjectsById.get(assignment.getClassSubjectId());
            Long classId = classSubject.getClassId();
            assertEquals(semester.getId(), classSubject.getSemesterId());
            assertTrue(classIdsByCode.containsValue(classId), "entry must target a fixture class");
            assertTrue(classSlots.add(classId + "|" + entry.getPeriodId()),
                    semesterCode + " has a class conflict");
            assertTrue(teacherSlots.add(assignment.getTeacherId() + "|" + entry.getPeriodId()),
                    semesterCode + " has a teacher conflict");
            String subjectCode = subjectCodesById.get(classSubject.getSubjectId());
            if (Set.of("TIN_HOC", "NGHE_DIEN", "NGHE_NONG_NGHIEP").contains(subjectCode)) {
                assertTrue(entry.getFunctionalRoomId() != null,
                        subjectCode + " entries must have a functional room");
            }
            if (entry.getFunctionalRoomId() != null) {
                assertTrue(roomSlots.add(entry.getFunctionalRoomId() + "|" + entry.getPeriodId()),
                        semesterCode + " has a functional-room conflict");
            }
        }
        Map<String, Long> entriesByClass = entries.stream()
                .collect(Collectors.groupingBy(entry -> classCodesById.get(
                        classSubjectsById.get(assignmentsById.get(entry.getAssignmentId()).getClassSubjectId())
                                .getClassId()),
                        Collectors.counting()));
        assertEquals(expectedFixtureClassCodes(), entriesByClass.keySet(),
                semesterCode + " entries must cover all 16 classes");
        assertTrue(entriesByClass.values().stream().allMatch(count -> count > 0),
                semesterCode + " every fixture class must have subject entries");
        Map<Long, Long> actualEntriesByAssignment = entries.stream()
                .collect(Collectors.groupingBy(TimetableEntry::getAssignmentId, Collectors.counting()));
        Map<Long, Long> expectedEntriesByAssignment = assignmentsById.values().stream()
                .filter(assignment -> semester.getId().equals(
                        classSubjectsById.get(assignment.getClassSubjectId()).getSemesterId()))
                .map(assignment -> Map.entry(assignment.getId(), weeklyPeriods(
                        assignment, classSubjectsById,
                        classIdsByCode.entrySet().stream().collect(Collectors.toMap(
                                entry -> entry.getValue(), entry -> entry.getKey())),
                        subjectCodesById, semesterCodesById)))
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (long) entry.getValue()));
        assertEquals(expectedEntriesByAssignment, actualEntriesByAssignment,
                semesterCode + " subject entries must match the design period counts");
        assertEquals(expectedEntryCount, actualEntriesByAssignment.values().stream()
                .mapToLong(Long::longValue).sum());

        Map<Long, String> roomCodesById = functionalRoomRepository.findAll().stream()
                .collect(Collectors.toMap(FunctionalRoom::getId, FunctionalRoom::getCode));
        Map<Long, Long> tinPeakByPeriod = entries.stream()
                .filter(entry -> "TIN-1".equals(roomCodesById.get(entry.getFunctionalRoomId()))
                        || "TIN-2".equals(roomCodesById.get(entry.getFunctionalRoomId())))
                .collect(Collectors.groupingBy(TimetableEntry::getPeriodId, Collectors.counting()));
        assertEquals(2L, tinPeakByPeriod.values().stream().mapToLong(Long::longValue).max().orElse(0L),
                semesterCode + " Tin room peak must be exactly 2");
        Map<Long, Long> nghềPeakByPeriod = entries.stream()
                .filter(entry -> "NGHE-1".equals(roomCodesById.get(entry.getFunctionalRoomId())))
                .collect(Collectors.groupingBy(TimetableEntry::getPeriodId, Collectors.counting()));
        if ("HK1".equals(semesterCode)) {
            assertTrue(nghềPeakByPeriod.isEmpty(), "HK1 must not contain vocational subjects");
        } else {
            assertEquals(1L, nghềPeakByPeriod.values().stream()
                    .mapToLong(Long::longValue).max().orElse(0L),
                    "HK2 vocational room peak must be exactly 1");
        }
        List<TimetableAudit> audits = timetableAuditRepository.findByRevisionIdOrderByCreatedAtDesc(revision.getId());
        assertTrue(audits.stream().anyMatch(audit -> "SEED_PLAN_081_FULL_V2".equals(audit.getAction())
                && audit.getDetails().contains("semester=" + semesterCode)),
                semesterCode + " must include the full timetable audit marker");
        return new TimetableSnapshot(
                head.getId(),
                head.getCurrentRevisionId(),
                revision.getId(),
                revision.getRevisionNumber(),
                entries.stream().map(TimetableEntry::getId).collect(Collectors.toSet()),
                entries.stream().collect(Collectors.toMap(
                        TimetableEntry::getId,
                        entry -> entry.getFunctionalRoomId() == null ? 0L : entry.getFunctionalRoomId())),
                audits.size());
    }

    private void assertTimetableUnchanged(TimetableSnapshot before, String semesterCode) {
        var head = timetableHeadRepository.findById(before.headId()).orElseThrow();
        List<TimetableRevision> revisions = timetableRevisionRepository
                .findByTimetableIdOrderByRevisionNumberDesc(head.getId());
        assertEquals(1, revisions.size(), semesterCode + " rerun must not create a revision");
        assertEquals(before.currentRevisionId(), head.getCurrentRevisionId());
        assertEquals(before.revisionId(), revisions.get(0).getId());
        assertEquals(before.revisionNumber(), revisions.get(0).getRevisionNumber());
        assertEquals(before.entryIds(), timetableEntryRepository.findByRevisionId(before.revisionId()).stream()
                .map(TimetableEntry::getId).collect(Collectors.toSet()),
                semesterCode + " rerun must not duplicate entries");
        assertEquals(before.roomIdsByEntry(), timetableEntryRepository.findByRevisionId(before.revisionId()).stream()
                .collect(Collectors.toMap(
                        TimetableEntry::getId,
                        entry -> entry.getFunctionalRoomId() == null ? 0L : entry.getFunctionalRoomId())),
                semesterCode + " rerun must preserve room bindings");
        assertEquals(before.auditCount(), timetableAuditRepository.findByRevisionIdOrderByCreatedAtDesc(
                before.revisionId()).size(), semesterCode + " rerun must not duplicate audit rows");
    }

    @Test
    void seedsFunctionalRoomsAndSubjectMappings() {
        runAllDemoSeeders();

        Set<String> expectedRooms = Set.of(
                "LAB-PHY-01", "LAB-CHEM-01", "LAB-BIO-01",
                "TIN-1", "TIN-2", "NGHE-1");
        assertEquals(expectedRooms,
                functionalRoomRepository.findAll().stream()
                        .map(room -> room.getCode()).collect(Collectors.toSet()));
        Map<String, FunctionalRoom> roomsByCode = functionalRoomRepository.findAll().stream()
                .collect(Collectors.toMap(FunctionalRoom::getCode, room -> room));
        assertTrue(expectedRooms.stream().allMatch(code -> roomsByCode.get(code).getStatus() == RoomStatus.ACTIVE));

        Map<String, Long> subjectsByCode = subjectRepository.findAll().stream()
                .collect(Collectors.toMap(subject -> subject.getCode(), subject -> subject.getId()));
        Map<Long, String> roomsById = roomsByCode.values().stream()
                .collect(Collectors.toMap(FunctionalRoom::getId, FunctionalRoom::getCode));
        assertEquals(Set.of("TIN-1", "TIN-2"), subjectFunctionalRoomRepository
                .findBySubjectId(subjectsByCode.get("TIN_HOC")).stream()
                .map(mapping -> roomsById.get(mapping.getFunctionalRoomId())).collect(Collectors.toSet()));
        for (String subjectCode : List.of("NGHE_DIEN", "NGHE_NONG_NGHIEP")) {
            assertEquals(Set.of("NGHE-1"), subjectFunctionalRoomRepository
                    .findBySubjectId(subjectsByCode.get(subjectCode)).stream()
                    .map(mapping -> roomsById.get(mapping.getFunctionalRoomId())).collect(Collectors.toSet()));
        }

        long roomCount = functionalRoomRepository.count();
        functionalRoomSeeder.run(new DefaultApplicationArguments());
        assertEquals(roomCount, functionalRoomRepository.count(), "functional room seed must be idempotent");
    }

    @Test
    void seedsHistoricalGradeSixAcademicRecordForUnassignedGradeSevenStudents() {
        runAllDemoSeeders();

        AcademicYear historicalYear = academicYearRepository.findAll().stream()
                .filter(year -> "2025-2026".equals(year.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(AcademicYearStatus.CLOSED, historicalYear.getStatus());
        Set<String> targetCodes = new HashSet<>();
        for (int sequence = 41; sequence <= 80; sequence++) {
            targetCodes.add(String.format("STU260%04d", sequence));
        }
        List<Student> targetStudents = studentRepository.findAllByStudentCodeIn(targetCodes);
        Set<Long> targetStudentIds = targetStudents.stream().map(Student::getId).collect(Collectors.toSet());

        Set<Long> historicalClassIds = schoolClassRepository.findAll().stream()
                .filter(schoolClass -> historicalYear.getId().equals(schoolClass.getAcademicYearId()))
                .filter(schoolClass -> Set.of("6A1", "6A2", "6A3", "6A4").contains(schoolClass.getClassCode()))
                .map(schoolClass -> schoolClass.getId()).collect(Collectors.toSet());
        assertEquals(4, historicalClassIds.size());
        List<StudentYearEnrollment> historicalEnrollments = enrollmentRepository.findAll().stream()
                .filter(enrollment -> historicalYear.getId().equals(enrollment.getAcademicYearId()))
                .filter(enrollment -> targetStudentIds.contains(enrollment.getStudentId()))
                .toList();
        assertEquals(40, historicalEnrollments.size());
        assertTrue(historicalEnrollments.stream().allMatch(e -> e.getStatus() == EnrollmentStatus.COMPLETED));

        Set<Long> historicalClassSubjectIds = classSubjectRepository.findAll().stream()
                .filter(classSubject -> historicalClassIds.contains(classSubject.getClassId()))
                .map(ClassSubject::getId).collect(Collectors.toSet());
        assertEquals(80, historicalClassSubjectIds.size());
        Set<Long> historicalScorebookIds = scorebookRepository.findAll().stream()
                .filter(scorebook -> historicalClassSubjectIds.contains(scorebook.getClassSubjectId()))
                .map(scorebook -> scorebook.getId()).collect(Collectors.toSet());
        assertEquals(80, historicalScorebookIds.size());
        Set<Long> historicalColumnIds = assessmentColumnRepository.findAll().stream()
                .filter(column -> historicalScorebookIds.contains(column.getScorebookId()))
                .map(column -> column.getId()).collect(Collectors.toSet());
        assertEquals(320, historicalColumnIds.size());
        List<StudentScore> historicalScores = studentScoreRepository.findAll().stream()
                .filter(score -> targetStudentIds.contains(score.getStudentId()))
                .filter(score -> historicalColumnIds.contains(score.getAssessmentColumnId()))
                .toList();
        assertEquals(3200, historicalScores.size());
        assertTrue(historicalScores.stream().allMatch(score -> score.getScoreStatus() == ScoreStatus.SCORED
                && score.getScoreValue().compareTo(BigDecimal.valueOf(6.5)) >= 0
                && score.getScoreValue().compareTo(BigDecimal.valueOf(9.5)) <= 0));

        List<StudentAnnualTranscript> annualTranscripts = annualTranscriptRepository
                .findAllByAcademicYearIdAndStudentIdIn(historicalYear.getId(), targetStudentIds);
        assertEquals(40, annualTranscripts.size());
        assertTrue(annualTranscripts.stream().allMatch(transcript ->
                transcript.getCalculationStatus() == CalculationStatus.FINISH
                        && transcript.getSourceVersion().equals(transcript.getCalculatedVersion())
                        && transcript.getFinalDtbcn().compareTo(BigDecimal.valueOf(6.5)) >= 0));
        Set<Long> annualTranscriptIds = annualTranscripts.stream()
                .map(StudentAnnualTranscript::getId).collect(Collectors.toSet());
        List<StudentTermTranscript> termTranscripts = termTranscriptRepository.findAll().stream()
                .filter(term -> annualTranscriptIds.contains(term.getAnnualTranscriptId()))
                .toList();
        assertEquals(80, termTranscripts.size());
        assertTrue(termTranscripts.stream().allMatch(term -> term.getCalculationStatus() == CalculationStatus.FINISH));
        Set<Long> termTranscriptIds = termTranscripts.stream().map(StudentTermTranscript::getId)
                .collect(Collectors.toSet());
        assertEquals(800, termResultRepository.findAll().stream()
                .filter(result -> termTranscriptIds.contains(result.getTermTranscriptId())).count());
        assertEquals(400, annualResultRepository.findAll().stream()
                .filter(result -> annualTranscriptIds.contains(result.getAnnualTranscriptId())).count());

        AcademicYear currentYear = academicYearRepository.findAll().stream()
                .filter(year -> "2026-2027".equals(year.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, enrollmentRepository.findAll().stream()
                .filter(enrollment -> currentYear.getId().equals(enrollment.getAcademicYearId()))
                .filter(enrollment -> targetStudentIds.contains(enrollment.getStudentId()))
                .count());

        Student targetStudent = targetStudents.stream()
                .filter(student -> "STU2600048".equals(student.getStudentCode()))
                .findFirst().orElseThrow();
        StudentAnnualTranscript targetAnnual = annualTranscriptRepository
                .findByStudentIdAndAcademicYearId(targetStudent.getId(), historicalYear.getId()).orElseThrow();
        assertEquals(CalculationStatus.FINISH, targetAnnual.getCalculationStatus());
        assertEquals(targetAnnual.getSourceVersion(), targetAnnual.getCalculatedVersion());
        assertEquals(0, new BigDecimal("8.2").compareTo(targetAnnual.getFinalDtbcn()));
        List<StudentScore> targetScores = historicalScores.stream()
                .filter(score -> targetStudent.getId().equals(score.getStudentId())).toList();
        List<String> targetScoreSnapshot = targetScores.stream()
                .map(score -> score.getAssessmentColumnId() + "|" + score.getScoreStatus() + "|"
                        + score.getScoreValue().toPlainString())
                .sorted().toList();
        List<String> targetAnnualResults = annualResultRepository
                .findAllByAnnualTranscriptIdOrderBySubjectIdAsc(targetAnnual.getId()).stream()
                .map(result -> result.getSubjectId() + "|" + result.getOfficialDtbmhCn().toPlainString())
                .toList();
        assertEquals(10, targetAnnualResults.size());

        long enrollmentCount = historicalEnrollments.size();
        long scoreCount = historicalScores.size();
        long annualCount = annualTranscripts.size();
        long termCount = termTranscripts.size();
        long termResultCount = termResultRepository.count();
        long annualResultCount = annualResultRepository.count();
        runAllDemoSeeders();
        assertEquals(enrollmentCount, enrollmentRepository.findAll().stream()
                .filter(enrollment -> historicalYear.getId().equals(enrollment.getAcademicYearId()))
                .filter(enrollment -> targetStudentIds.contains(enrollment.getStudentId())).count());
        assertEquals(scoreCount, studentScoreRepository.findAll().stream()
                .filter(score -> targetStudentIds.contains(score.getStudentId()))
                .filter(score -> historicalColumnIds.contains(score.getAssessmentColumnId())).count());
        assertEquals(annualCount, annualTranscriptRepository
                .findAllByAcademicYearIdAndStudentIdIn(historicalYear.getId(), targetStudentIds).size());
        assertEquals(termCount, termTranscriptRepository.findAll().stream()
                .filter(term -> annualTranscriptIds.contains(term.getAnnualTranscriptId())).count());
        assertEquals(termResultCount, termResultRepository.count());
        assertEquals(annualResultCount, annualResultRepository.count());
        StudentAnnualTranscript targetAnnualAfterReseed = annualTranscriptRepository
                .findByStudentIdAndAcademicYearId(targetStudent.getId(), historicalYear.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("8.2").compareTo(targetAnnualAfterReseed.getFinalDtbcn()));
        List<String> targetScoreSnapshotAfterReseed = studentScoreRepository.findAll().stream()
                .filter(score -> targetStudent.getId().equals(score.getStudentId()))
                .filter(score -> historicalColumnIds.contains(score.getAssessmentColumnId()))
                .map(score -> score.getAssessmentColumnId() + "|" + score.getScoreStatus() + "|"
                        + score.getScoreValue().toPlainString())
                .sorted().toList();
        assertEquals(targetScoreSnapshot, targetScoreSnapshotAfterReseed,
                "re-running demo seeders must preserve STU2600048 subject assessment scores");
        List<String> targetAnnualResultsAfterReseed = annualResultRepository
                .findAllByAnnualTranscriptIdOrderBySubjectIdAsc(targetAnnualAfterReseed.getId()).stream()
                .map(result -> result.getSubjectId() + "|" + result.getOfficialDtbmhCn().toPlainString())
                .toList();
        assertEquals(targetAnnualResults, targetAnnualResultsAfterReseed,
                "re-running demo seeders must preserve STU2600048 annual subject averages");
    }

    @Test
    void seedsScorebookColumnsAndNullValuesForNonScoredStatuses() {
        runAllDemoSeeders();

        assertEquals(1, scorebookRepository.count());
        List<Long> scorebookIds = scorebookRepository.findAll().stream()
                .map(scorebook -> scorebook.getId()).toList();
        assertEquals(4,
                assessmentColumnRepository.findAllByScorebookIdInOrderByScorebookIdAscAssessmentTypeAscColumnNoAsc(
                        scorebookIds).size());
        assertEquals(40, studentScoreRepository.count());

        List<com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore> nonScored = studentScoreRepository
                .findAll().stream()
                .filter(score -> score.getScoreStatus() != ScoreStatus.SCORED)
                .toList();
        assertEquals(3, nonScored.size());
        assertTrue(nonScored.stream().allMatch(score -> score.getScoreValue() == null),
                "ABSENT, EXEMPTED, and CANCELLED demo scores must have null score values");

        long scorebookCount = scorebookRepository.count();
        long columnCount = assessmentColumnRepository.count();
        long scoreCount = studentScoreRepository.count();
        scorebookSeeder.run(new DefaultApplicationArguments());
        assertEquals(scorebookCount, scorebookRepository.count());
        assertEquals(columnCount, assessmentColumnRepository.count());
        assertEquals(scoreCount, studentScoreRepository.count());
    }

    @Test
    void seedsPlacementSessionsCandidatesResultsAndIsIdempotent() {
        runAllDemoSeeders();

        Map<String, PlacementSessionStatus> statuses = placementSessionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        session -> extractPlacementSeedKey(session.getScopeSnapshot()),
                        session -> session.getStatus()));

        assertEquals(Set.of("PLACE-081-DRAFT", "PLACE-081-SIM", "PLACE-081-READY",
                        "PLACE-081-CONFIRMED", "PLACE-081-CANCELLED"), statuses.keySet());
        assertEquals(Set.of(PlacementSessionStatus.DRAFT, PlacementSessionStatus.SIMULATED,
                        PlacementSessionStatus.READY_FOR_CONFIRM, PlacementSessionStatus.CONFIRMED,
                        PlacementSessionStatus.CANCELLED), Set.copyOf(statuses.values()));
        assertEquals(60, placementCandidateRepository.count());
        assertEquals(60, placementResultRepository.count());
        assertEquals(20, placementResultRepository.findAll().stream()
                .filter(result -> result.getResultStatus() == PlacementResultStatus.MANUAL_REQUIRED)
                .count());
        assertEquals(40, placementResultRepository.findAll().stream()
                .filter(result -> result.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED)
                .count());

        long sessions = placementSessionRepository.count();
        long candidates = placementCandidateRepository.count();
        long results = placementResultRepository.count();
        runAllDemoSeeders();
        assertEquals(sessions, placementSessionRepository.count());
        assertEquals(candidates, placementCandidateRepository.count());
        assertEquals(results, placementResultRepository.count());
    }

    @Test
    void seedsNotificationIdempotencyAndReceipts() {
        runAllDemoSeeders();

        assertEquals(8, notificationRepository.count());
        assertEquals(8, notificationRepository.findAll().stream()
                .map(notification -> notification.getIdempotencyKey())
                .distinct().count());
        assertEquals(Set.of("NOTI-081-IND-01", "NOTI-081-IND-02", "NOTI-081-CLASS-01",
                        "NOTI-081-SCHOOL-01", "NOTI-081-EMAIL-01", "NOTI-081-EXPIRED",
                        "NOTI-081-CANCELLED", "NOTI-081-DRAFT"), notificationRepository.findAll().stream()
                .map(notification -> notification.getIdempotencyKey()).collect(Collectors.toSet()));
        assertEquals(6, notificationReceiptRepository.count());
        assertEquals(4, notificationRepository.findAll().stream()
                .filter(notification -> notification.getStatus() == NotificationStatus.PUBLISHED)
                .count());

        long notifications = notificationRepository.count();
        long receipts = notificationReceiptRepository.count();
        runAllDemoSeeders();
        assertEquals(notifications, notificationRepository.count());
        assertEquals(receipts, notificationReceiptRepository.count());
    }

    private void runAllDemoSeeders() {
        demoDataSeeder.run(new DefaultApplicationArguments());
        functionalRoomSeeder.run(new DefaultApplicationArguments());
        placementSeeder.run(new DefaultApplicationArguments());
        notificationSeeder.run(new DefaultApplicationArguments());
        scorebookSeeder.run(new DefaultApplicationArguments());
    }

    private String extractPlacementSeedKey(String scopeSnapshot) {
        return Set.of("PLACE-081-DRAFT", "PLACE-081-SIM", "PLACE-081-READY",
                        "PLACE-081-CONFIRMED", "PLACE-081-CANCELLED").stream()
                .filter(scopeSnapshot::contains)
                .findFirst()
                .orElse(scopeSnapshot);
    }

    private Map<String, TeacherFixture> canonicalTeachers() {
        return Map.ofEntries(
                Map.entry("GV001", new TeacherFixture("pham.minh.quan", "Phạm Minh Quân")),
                Map.entry("GV002", new TeacherFixture("tran.thu.ha", "Trần Thu Hà")),
                Map.entry("GV003", new TeacherFixture("le.hoang.nam", "Lê Hoàng Nam")),
                Map.entry("GV004", new TeacherFixture("pham.ngoc.lan", "Phạm Ngọc Lan")),
                Map.entry("GV005", new TeacherFixture("tran.quoc.bao", "Trần Quốc Bảo")),
                Map.entry("GV006", new TeacherFixture("le.thi.huong", "Lê Thị Hương")),
                Map.entry("GV007", new TeacherFixture("pham.duc.anh", "Phạm Đức Anh")),
                Map.entry("GV008", new TeacherFixture("tran.mai.phuong", "Trần Mai Phương")),
                Map.entry("GV009", new TeacherFixture("le.van.thanh", "Lê Văn Thành")),
                Map.entry("GV010", new TeacherFixture("pham.thuy.dung", "Phạm Thùy Dung")),
                Map.entry("GV011", new TeacherFixture("tran.minh.khoi", "Trần Minh Khôi")),
                Map.entry("GV012", new TeacherFixture("le.ngoc.mai", "Lê Ngọc Mai")),
                Map.entry("GV013", new TeacherFixture("pham.huu.dat", "Phạm Hữu Đạt")),
                Map.entry("GV014", new TeacherFixture("tran.thanh.van", "Trần Thanh Vân")),
                Map.entry("GV015", new TeacherFixture("le.cong.thanh", "Lê Công Thành")),
                Map.entry("GV016", new TeacherFixture("pham.khanh.linh", "Phạm Khánh Linh")),
                Map.entry("GV017", new TeacherFixture("tran.anh.dung", "Trần Anh Dũng")),
                Map.entry("GV018", new TeacherFixture("le.thu.trang", "Lê Thu Trang")),
                Map.entry("GV019", new TeacherFixture("pham.tuan.kiet", "Phạm Tuấn Kiệt")),
                Map.entry("GV020", new TeacherFixture("tran.hai.yen", "Trần Hải Yến")));
    }

    private Map<String, String> canonicalHomerooms() {
        return Map.ofEntries(
                Map.entry("6A1", "GV001"),
                Map.entry("6A2", "GV002"),
                Map.entry("6A3", "GV003"),
                Map.entry("6A4", "GV004"),
                Map.entry("7A1", "GV005"),
                Map.entry("7A2", "GV006"),
                Map.entry("7A3", "GV007"),
                Map.entry("7A4", "GV008"),
                Map.entry("8A1", "GV009"),
                Map.entry("8A2", "GV010"),
                Map.entry("8A3", "GV011"),
                Map.entry("8A4", "GV012"),
                Map.entry("9A1", "GV013"),
                Map.entry("9A2", "GV014"),
                Map.entry("9A3", "GV015"),
                Map.entry("9A4", "GV016"));
    }

    private String applicabilityKey(
            SubjectApplicability applicability,
            Map<Long, String> subjectCodesById,
            Map<Long, String> semesterCodesById,
            Map<Long, Integer> gradeById) {
        return resolveCode(subjectCodesById, applicability.getSubjectId(), "subject") + "|"
                + gradeById.get(applicability.getGradeLevelId()) + "|"
                + resolveCode(semesterCodesById, applicability.getSemesterId(), "semester");
    }

    private int weeklyPeriods(
            SubjectTeachingAssignment assignment,
            Map<Long, ClassSubject> classSubjectsById,
            Map<Long, String> classCodesById,
            Map<Long, String> subjectCodesById,
            Map<Long, String> semesterCodesById) {
        ClassSubject classSubject = classSubjectsById.get(assignment.getClassSubjectId());
        int grade = Integer.parseInt(resolveCode(classCodesById, classSubject.getClassId(), "class").substring(0, 1));
        String subjectCode = resolveCode(subjectCodesById, classSubject.getSubjectId(), "subject");
        String semesterCode = resolveCode(semesterCodesById, classSubject.getSemesterId(), "semester");
        return switch (subjectCode) {
            case "TOAN" -> gradePeriods(grade, 4, 4, 4, 4);
            case "VAT_LY" -> gradePeriods(grade, 2, 2, 2, 2);
            case "HOA_HOC" -> gradePeriods(grade, 0, 0, 2, 2);
            case "SINH_HOC" -> gradePeriods(grade, 2, 2, 2, 2);
            case "NGU_VAN" -> gradePeriods(grade, 4, 4, 4, 5);
            case "NGOAI_NGU" -> gradePeriods(grade, 3, 3, 3, 2);
            case "LICH_SU" -> gradePeriods(grade, 1, 2, 2, 1);
            case "DIA_LY" -> gradePeriods(grade, 1, 2, 2, 1);
            case "GDCD" -> gradePeriods(grade, 1, 1, 1, 1);
            case "TIN_HOC" -> gradePeriods(grade, 2, 2, 2, 2);
            case "CONG_NGHE" -> gradePeriods(grade, 2, 2, 1, "HK2".equals(semesterCode) ? 0 : 1);
            case "NGHE_DIEN", "NGHE_NONG_NGHIEP" -> "HK2".equals(semesterCode) ? 2 : 0;
            default -> throw new IllegalArgumentException("Unknown subject in weekly-period rules: " + subjectCode);
        };
    }

    private int gradePeriods(int grade, int grade6, int grade7, int grade8, int grade9) {
        return switch (grade) {
            case 6 -> grade6;
            case 7 -> grade7;
            case 8 -> grade8;
            case 9 -> grade9;
            default -> throw new IllegalArgumentException("Unknown grade in weekly-period rules: " + grade);
        };
    }

    private int expectedMinimumLoad(String teacherCode, Map<String, String> homerooms) {
        return homerooms.containsValue(teacherCode) ? 15 : 19;
    }

    private String resolveCode(Map<Long, String> codesById, Long id, String itemType) {
        return codesById.getOrDefault(id, "MISSING_" + itemType + "_ID_" + id);
    }

    private void assertExactSet(Set<String> expected, Set<String> actual, String fixtureName) {
        Set<String> missing = expected.stream().filter(code -> !actual.contains(code)).collect(Collectors.toSet());
        Set<String> unexpected = actual.stream().filter(code -> !expected.contains(code)).collect(Collectors.toSet());
        assertEquals(expected, actual,
                fixtureName + " mismatch; missing=" + missing + ", unexpected=" + unexpected);
    }

    private Set<String> expectedFixtureClassCodes() {
        return Set.of(
                "6A1", "6A2", "6A3", "6A4",
                "7A1", "7A2", "7A3", "7A4",
                "8A1", "8A2", "8A3", "8A4",
                "9A1", "9A2", "9A3", "9A4");
    }

    private record TeacherFixture(String username, String name) {
    }

    private record TimetableSnapshot(
            Long headId,
            Long currentRevisionId,
            Long revisionId,
            Integer revisionNumber,
            Set<Long> entryIds,
            Map<Long, Long> roomIdsByEntry,
            int auditCount) {
    }
}
