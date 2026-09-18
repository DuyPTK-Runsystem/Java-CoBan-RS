package com.JavaTraining.BaiTap_RS.bootstrap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
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
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private ScorebookRepository scorebookRepository;

    @Autowired
    private AssessmentColumnRepository assessmentColumnRepository;

    @Autowired
    private StudentScoreRepository studentScoreRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsDeterministicIdentityAcademicAndAssignmentFixture() {
        runAllDemoSeeders();

        assertTrue(userRepository.count() == 182
                && teacherRepository.count() == 20
                && studentRepository.count() == 160
                && enrollmentRepository.count() == 160
                && schoolClassRepository.count() == 16
                && enrollmentRepository.findAll().stream()
                .collect(Collectors.groupingBy(StudentYearEnrollment::getCurrentClassId, Collectors.counting()))
                .size() == 16
                && enrollmentRepository.findAll().stream()
                .collect(Collectors.groupingBy(StudentYearEnrollment::getCurrentClassId, Collectors.counting()))
                .values().stream().allMatch(count -> count == 10)
                && schoolClassRepository.findAll().stream().allMatch(schoolClass -> schoolClass.getCapacity() == 10)
                && homeroomAssignmentRepository.count() == 16
                && applicabilityRepository.count() == 91
                && classSubjectRepository.count() == 364
                && teachingAssignmentRepository.count() == 364
                && userRepository.findByUsername(ADMIN_USERNAME)
                .filter(user -> passwordEncoder.matches(ADMIN_PASSWORD, user.getPassword()))
                .isPresent()
                && userRepository.findByUsername("academic.office")
                .filter(user -> passwordEncoder.matches(DEFAULT_PASSWORD, user.getPassword())
                        && user.getRoles().stream().anyMatch(role -> "ACADEMIC_OFFICE".equals(role.getCode())))
                .isPresent()
                && userRepository.findByUsername("nguyen.minh.khang61")
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
                    && fixture.username().equals(userRepository.findById(teacher.getUserId()).orElseThrow().getUsername())
                    && (fixture.username() + "@example.test").equals(teacher.getEmail());
        }), "canonical teacher emails must be deterministic");
    }

    @Test
    void seedsFourFunctionalRooms() {
        runAllDemoSeeders();

        assertEquals(Set.of("LAB-PHY-01", "LAB-CHEM-01", "LAB-IT-01", "LAB-BIO-01"),
                functionalRoomRepository.findAll().stream()
                        .map(room -> room.getCode()).collect(Collectors.toSet()));
        assertTrue(functionalRoomRepository.findAll().stream()
                .allMatch(room -> "ACTIVE".equals(room.getStatus().name())));

        long roomCount = functionalRoomRepository.count();
        functionalRoomSeeder.run(new DefaultApplicationArguments());
        assertEquals(roomCount, functionalRoomRepository.count(), "functional room seed must be idempotent");
    }

    @Test
    void seedsScorebookColumnsAndNullValuesForNonScoredStatuses() {
        runAllDemoSeeders();

        assertEquals(1, scorebookRepository.count());
        List<Long> scorebookIds = scorebookRepository.findAll().stream()
                .map(scorebook -> scorebook.getId()).toList();
        assertEquals(4, assessmentColumnRepository.findAllByScorebookIdInOrderByScorebookIdAscAssessmentTypeAscColumnNoAsc(
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

    private record TeacherFixture(String username, String name) {
    }
}
