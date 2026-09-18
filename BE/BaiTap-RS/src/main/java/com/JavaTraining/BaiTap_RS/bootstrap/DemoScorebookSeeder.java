package com.JavaTraining.BaiTap_RS.bootstrap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.seed.demo.enabled", havingValue = "true")
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "PMD.CouplingBetweenObjects",
        "PMD.LambdaCanBeMethodReference",
        "PMD.AvoidInstantiatingObjectsInLoops"
})
public class DemoScorebookSeeder implements ApplicationRunner {

    private static final String ACADEMIC_YEAR_CODE = "2026-2027";
    private static final String SEMESTER_CODE = "HK1";
    private static final String CLASS_CODE = "6A1";
    private static final String SUBJECT_CODE = "TOAN";
    private static final String ACADEMIC_OFFICE_USERNAME = "academic.office";
    private static final String ADMIN_USERNAME = "admin";

    private static final List<ColumnSeed> COLUMN_SEEDS = List.of(
            new ColumnSeed(AssessmentType.KTTT, 1, "Miệng 1", "1.00"),
            new ColumnSeed(AssessmentType.KTTT, 2, "15 phút 1", "1.00"),
            new ColumnSeed(AssessmentType.KTDK, 1, "Giữa kỳ", "2.00"),
            new ColumnSeed(AssessmentType.KTCK, 1, "Cuối kỳ", "3.00"));

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ScorebookRepository scorebookRepository;
    private final AssessmentColumnRepository columnRepository;
    private final StudentScoreRepository scoreRepository;

    public DemoScorebookSeeder(
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            ClassSubjectRepository classSubjectRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            ScorebookRepository scorebookRepository,
            AssessmentColumnRepository columnRepository,
            StudentScoreRepository scoreRepository) {
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.scorebookRepository = scorebookRepository;
        this.columnRepository = columnRepository;
        this.scoreRepository = scoreRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<Long> actorId = findActorId();
        Optional<ClassSubject> target = findTargetClassSubject();
        if (actorId.isEmpty() || target.isEmpty()) {
            return;
        }

        List<StudentYearEnrollment> enrollments = enrollmentRepository
                .findByCurrentClassIdAndStatusOrderByStudentIdAsc(target.get().getClassId(), EnrollmentStatus.ACTIVE);
        if (enrollments.isEmpty()) {
            return;
        }

        Scorebook scorebook = scorebookRepository.findByClassSubjectId(target.get().getId())
                .orElseGet(() -> scorebookRepository.save(
                        new Scorebook(target.get().getId(), ScorebookStatus.DRAFT)));
        List<AssessmentColumn> columns = seedColumns(scorebook.getId());
        seedScores(columns, enrollments, actorId.get());
    }

    private Optional<Long> findActorId() {
        return userRepository.findByUsername(ACADEMIC_OFFICE_USERNAME)
                .or(() -> userRepository.findByUsername(ADMIN_USERNAME))
                .map(User::getId);
    }

    private Optional<ClassSubject> findTargetClassSubject() {
        Optional<AcademicYear> academicYear = academicYearRepository.findAll().stream()
                .filter(year -> ACADEMIC_YEAR_CODE.equals(year.getCode()))
                .findFirst();
        if (academicYear.isEmpty()) {
            return Optional.empty();
        }

        Optional<Semester> semester = semesterRepository
                .findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYear.get().getId()).stream()
                .filter(item -> SEMESTER_CODE.equals(item.getCode()))
                .findFirst();
        Optional<Long> classId = schoolClassRepository
                .findAllByAcademicYearIdOrderByClassCodeAsc(academicYear.get().getId()).stream()
                .filter(item -> CLASS_CODE.equals(item.getClassCode()))
                .map(item -> item.getId())
                .findFirst();
        Optional<Long> subjectId = subjectRepository.findAllByOrderByCodeAsc().stream()
                .filter(item -> SUBJECT_CODE.equals(item.getCode()))
                .map(item -> item.getId())
                .findFirst();
        if (semester.isEmpty() || classId.isEmpty() || subjectId.isEmpty()) {
            return Optional.empty();
        }

        return classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(
                        classId.get(), semester.get().getId()).stream()
                .filter(item -> subjectId.get().equals(item.getSubjectId()))
                .filter(item -> item.getStatus() == ClassSubjectStatus.ACTIVE)
                .findFirst();
    }

    private List<AssessmentColumn> seedColumns(Long scorebookId) {
        return COLUMN_SEEDS.stream()
                .map(seed -> findOrCreateColumn(scorebookId, seed))
                .toList();
    }

    private AssessmentColumn findOrCreateColumn(Long scorebookId, ColumnSeed seed) {
        return columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(scorebookId).stream()
                .filter(column -> column.getAssessmentType() == seed.type())
                .filter(column -> seed.columnNo() == column.getColumnNo())
                .findFirst()
                .orElseGet(() -> columnRepository.save(new AssessmentColumn(
                        scorebookId,
                        seed.type(),
                        seed.columnNo(),
                        seed.name(),
                        new BigDecimal(seed.weight()),
                        true)));
    }

    private void seedScores(List<AssessmentColumn> columns, List<StudentYearEnrollment> enrollments, Long actorId) {
        for (int studentIndex = 0; studentIndex < enrollments.size(); studentIndex++) {
            Long studentId = enrollments.get(studentIndex).getStudentId();
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                AssessmentColumn column = columns.get(columnIndex);
                if (scoreRepository.findByAssessmentColumnIdAndStudentId(column.getId(), studentId).isPresent()) {
                    continue;
                }
                ScoreSeed score = scoreFor(studentIndex, columnIndex);
                scoreRepository.save(new StudentScore(
                        column.getId(), studentId, score.status(), score.value(), score.note(), actorId));
            }
        }
    }

    private ScoreSeed scoreFor(int studentIndex, int columnIndex) {
        if (studentIndex == 1 && columnIndex == 1) {
            return new ScoreSeed(ScoreStatus.ABSENT, null, "Vắng kiểm tra");
        }
        if (studentIndex == 2 && columnIndex == 2) {
            return new ScoreSeed(ScoreStatus.EXEMPTED, null, "Được miễn kiểm tra");
        }
        if (studentIndex == 3 && columnIndex == 3) {
            return new ScoreSeed(ScoreStatus.CANCELLED, null, "Huỷ điểm demo");
        }
        if (studentIndex == 0) {
            return new ScoreSeed(ScoreStatus.SCORED, new BigDecimal[] {
                    new BigDecimal("0.0"), new BigDecimal("5.5"),
                    new BigDecimal("7.0"), new BigDecimal("9.2")
            }[columnIndex], null);
        }
        return new ScoreSeed(ScoreStatus.SCORED, BigDecimal.valueOf(5 + studentIndex % 6), null);
    }

    private record ColumnSeed(AssessmentType type, int columnNo, String name, String weight) {
    }

    private record ScoreSeed(ScoreStatus status, BigDecimal value, String note) {
    }
}
