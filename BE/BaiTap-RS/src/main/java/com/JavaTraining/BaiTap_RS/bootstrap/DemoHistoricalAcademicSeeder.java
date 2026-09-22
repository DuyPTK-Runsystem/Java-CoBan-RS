package com.JavaTraining.BaiTap_RS.bootstrap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
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
import com.JavaTraining.BaiTap_RS.scorebook.service.TranscriptRecalculationService;
import com.JavaTraining.BaiTap_RS.scorebook.service.TranscriptStateService;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@SuppressWarnings({
        "PMD.CouplingBetweenObjects",
        "PMD.ExcessiveImports",
        "PMD.ExcessiveParameterList",
        "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.CyclomaticComplexity",
        "PMD.GodClass",
        "PMD.TooManyMethods"
})
public class DemoHistoricalAcademicSeeder {

    private static final String YEAR_CODE = "2025-2026";
    private static final LocalDate YEAR_START = LocalDate.of(2025, 9, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2026, 5, 31);
    private static final LocalDate HK1_START = LocalDate.of(2025, 9, 1);
    private static final LocalDate HK1_END = LocalDate.of(2025, 12, 31);
    private static final LocalDate HK2_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate HK2_END = LocalDate.of(2026, 5, 31);
    private static final LocalDateTime ENROLLED_AT = LocalDateTime.of(2025, 9, 1, 8, 0);
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 5, 31, 23, 59, 59);
    private static final String STUDENT_CODE_PREFIX = "STU260";
    private static final int FIRST_STUDENT_NUMBER = 41;
    private static final int LAST_STUDENT_NUMBER = 80;
    private static final int STUDENTS_PER_CLASS = 10;
    private static final int GRADE = 6;
    private static final List<String> CLASS_CODES = List.of("6A1", "6A2", "6A3", "6A4");
    private static final List<String> SUBJECT_CODES = List.of(
            "TOAN", "VAT_LY", "SINH_HOC", "NGU_VAN", "NGOAI_NGU",
            "LICH_SU", "DIA_LY", "GDCD", "TIN_HOC", "CONG_NGHE");
    private static final List<ColumnSeed> COLUMN_SEEDS = List.of(
            new ColumnSeed(AssessmentType.KTTT, 1, "Miệng 1", "1.00"),
            new ColumnSeed(AssessmentType.KTTT, 2, "15 phút 1", "1.00"),
            new ColumnSeed(AssessmentType.KTDK, 1, "Giữa kỳ", "2.00"),
            new ColumnSeed(AssessmentType.KTCK, 1, "Cuối kỳ", "3.00"));

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectApplicabilityRepository applicabilityRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final ScorebookRepository scorebookRepository;
    private final AssessmentColumnRepository columnRepository;
    private final StudentScoreRepository scoreRepository;
    private final StudentAnnualTranscriptRepository annualTranscriptRepository;
    private final StudentTermTranscriptRepository termTranscriptRepository;
    private final StudentSubjectTermResultRepository termResultRepository;
    private final StudentSubjectAnnualResultRepository annualResultRepository;
    private final TranscriptStateService transcriptStateService;
    private final TranscriptRecalculationService recalculationService;

    public DemoHistoricalAcademicSeeder(
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectApplicabilityRepository applicabilityRepository,
            ClassSubjectRepository classSubjectRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            ScorebookRepository scorebookRepository,
            AssessmentColumnRepository columnRepository,
            StudentScoreRepository scoreRepository,
            StudentAnnualTranscriptRepository annualTranscriptRepository,
            StudentTermTranscriptRepository termTranscriptRepository,
            StudentSubjectTermResultRepository termResultRepository,
            StudentSubjectAnnualResultRepository annualResultRepository,
            TranscriptStateService transcriptStateService,
            TranscriptRecalculationService recalculationService) {
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.applicabilityRepository = applicabilityRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.scorebookRepository = scorebookRepository;
        this.columnRepository = columnRepository;
        this.scoreRepository = scoreRepository;
        this.annualTranscriptRepository = annualTranscriptRepository;
        this.termTranscriptRepository = termTranscriptRepository;
        this.termResultRepository = termResultRepository;
        this.annualResultRepository = annualResultRepository;
        this.transcriptStateService = transcriptStateService;
        this.recalculationService = recalculationService;
    }

    @Transactional
    public void seed(List<Student> currentStudents, GradeLevel gradeSix, Map<String, Subject> subjects, Long actorId) {
        AcademicYear academicYear = ensureAcademicYear();
        List<Semester> semesters = ensureSemesters(academicYear);
        List<SchoolClass> classes = ensureClasses(academicYear, gradeSix);
        Map<String, Subject> academicSubjects = selectSubjects(subjects);
        List<ClassSubject> classSubjects = ensureClassSubjects(classes, semesters, gradeSix, academicSubjects);
        Map<String, List<ClassSubject>> classSubjectsByStudentClass = indexClassSubjects(classes, semesters, classSubjects);
        List<Student> targetStudents = targetStudents(currentStudents);
        Map<String, StudentYearEnrollment> enrollments = ensureEnrollments(targetStudents, classes, academicYear);

        for (Student student : targetStudents) {
            StudentYearEnrollment enrollment = enrollments.get(student.getStudentCode());
            boolean scoreDataChanged = seedScorebooksAndScores(
                    student, enrollment, classes, semesters, classSubjectsByStudentClass, actorId);
            ensureTranscript(student, enrollment, academicYear, semesters, scoreDataChanged);
            completeEnrollment(enrollment);
        }
    }

    private AcademicYear ensureAcademicYear() {
        AcademicYear year = academicYearRepository.findAll().stream()
                .filter(existing -> YEAR_CODE.equals(existing.getCode()))
                .findFirst()
                .orElseGet(() -> academicYearRepository.save(new AcademicYear(
                        YEAR_CODE, YEAR_START, YEAR_END, AcademicYearStatus.CLOSED,
                        "Historical demo fixture for Grade 6 continuing placement")));
        year.setStartDate(YEAR_START);
        year.setEndDate(YEAR_END);
        year.setStatus(AcademicYearStatus.CLOSED);
        return academicYearRepository.save(year);
    }

    private List<Semester> ensureSemesters(AcademicYear academicYear) {
        return List.of(
                ensureSemester(academicYear, "HK1", "HK1 2025 - 2026", 1, HK1_START, HK1_END),
                ensureSemester(academicYear, "HK2", "HK2 2025 - 2026", 2, HK2_START, HK2_END));
    }

    private Semester ensureSemester(
            AcademicYear academicYear,
            String code,
            String name,
            int displayOrder,
            LocalDate startDate,
            LocalDate endDate) {
        Semester semester = semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYear.getId())
                .stream()
                .filter(existing -> code.equals(existing.getCode()))
                .findFirst()
                .orElseGet(() -> semesterRepository.save(new Semester(
                        academicYear.getId(), code, name, displayOrder, startDate, endDate,
                        null, SemesterStatus.CLOSED)));
        semester.setName(name);
        semester.setDisplayOrder(displayOrder);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semester.setStatus(SemesterStatus.CLOSED);
        return semesterRepository.save(semester);
    }

    private List<SchoolClass> ensureClasses(AcademicYear academicYear, GradeLevel gradeSix) {
        List<SchoolClass> classes = new ArrayList<>();
        for (String code : CLASS_CODES) {
            SchoolClass schoolClass = schoolClassRepository
                    .findAllByAcademicYearIdOrderByClassCodeAsc(academicYear.getId()).stream()
                    .filter(existing -> code.equals(existing.getClassCode()))
                    .findFirst()
                    .orElseGet(() -> schoolClassRepository.save(new SchoolClass(
                            academicYear.getId(), gradeSix.getId(), code, "Lớp " + code,
                            STUDENTS_PER_CLASS, SchoolClassStatus.ACTIVE)));
            schoolClass.setGradeLevelId(gradeSix.getId());
            schoolClass.setCapacity(STUDENTS_PER_CLASS);
            schoolClass.setStatus(SchoolClassStatus.ACTIVE);
            classes.add(schoolClassRepository.save(schoolClass));
        }
        classes.sort(Comparator.comparing(SchoolClass::getClassCode));
        return classes;
    }

    private Map<String, Subject> selectSubjects(Map<String, Subject> subjects) {
        Map<String, Subject> selected = new HashMap<>();
        for (String code : SUBJECT_CODES) {
            Subject subject = subjects.get(code);
            if (subject == null) {
                throw new IllegalStateException("Missing historical subject: " + code);
            }
            selected.put(code, subject);
        }
        return selected;
    }

    private List<ClassSubject> ensureClassSubjects(
            List<SchoolClass> classes,
            List<Semester> semesters,
            GradeLevel gradeSix,
            Map<String, Subject> subjects) {
        List<ClassSubject> result = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            for (Semester semester : semesters) {
                for (String subjectCode : SUBJECT_CODES) {
                    Subject subject = subjects.get(subjectCode);
                    ensureApplicability(subject, semester, gradeSix);
                    ClassSubject classSubject = classSubjectRepository
                            .findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(
                                    schoolClass.getId(), semester.getId()).stream()
                            .filter(existing -> Objects.equals(existing.getSubjectId(), subject.getId()))
                            .findFirst()
                            .orElseGet(() -> classSubjectRepository.save(new ClassSubject(
                                    schoolClass.getId(), subject.getId(), semester.getId(),
                                    ClassSubjectStatus.ACTIVE)));
                    classSubject.setStatus(ClassSubjectStatus.ACTIVE);
                    result.add(classSubjectRepository.save(classSubject));
                }
            }
        }
        return result;
    }

    private void ensureApplicability(Subject subject, Semester semester, GradeLevel gradeSix) {
        SubjectApplicability applicability = applicabilityRepository
                .findAllByFilters(subject.getId(), semester.getId(), null).stream()
                .filter(existing -> existing.getScopeType() == ApplicationScope.GRADE)
                .filter(existing -> Objects.equals(existing.getGradeLevelId(), gradeSix.getId()))
                .findFirst()
                .orElseGet(() -> new SubjectApplicability(
                        subject.getId(), semester.getId(), ApplicationScope.GRADE,
                        gradeSix.getId(), null, SubjectApplicabilityStatus.ACTIVE));
        applicability.setStatus(SubjectApplicabilityStatus.ACTIVE);
        applicabilityRepository.save(applicability);
    }

    private Map<String, List<ClassSubject>> indexClassSubjects(
            List<SchoolClass> classes,
            List<Semester> semesters,
            List<ClassSubject> classSubjects) {
        Map<Long, String> classCodes = classes.stream()
                .collect(java.util.stream.Collectors.toMap(SchoolClass::getId, SchoolClass::getClassCode));
        Map<Long, String> semesterCodes = semesters.stream()
                .collect(java.util.stream.Collectors.toMap(Semester::getId, Semester::getCode));
        Map<String, List<ClassSubject>> index = new HashMap<>();
        for (ClassSubject classSubject : classSubjects) {
            String key = classCodes.get(classSubject.getClassId()) + "|" + semesterCodes.get(classSubject.getSemesterId());
            index.computeIfAbsent(key, ignored -> new ArrayList<>()).add(classSubject);
        }
        return index;
    }

    private List<Student> targetStudents(List<Student> currentStudents) {
        List<Student> target = currentStudents.stream()
                .filter(student -> isTargetStudent(student.getStudentCode()))
                .sorted(Comparator.comparing(Student::getStudentCode))
                .toList();
        if (target.size() != LAST_STUDENT_NUMBER - FIRST_STUDENT_NUMBER + 1) {
            throw new IllegalStateException("Historical seed requires exactly 40 Grade 7 students");
        }
        return target;
    }

    private Map<String, StudentYearEnrollment> ensureEnrollments(
            List<Student> students,
            List<SchoolClass> classes,
            AcademicYear academicYear) {
        Map<String, StudentYearEnrollment> result = new HashMap<>();
        for (int index = 0; index < students.size(); index++) {
            Student student = students.get(index);
            SchoolClass schoolClass = classes.get(index / STUDENTS_PER_CLASS);
            StudentYearEnrollment enrollment = enrollmentRepository
                    .findByStudentIdAndAcademicYearId(student.getId(), academicYear.getId())
                    .orElseGet(() -> new StudentYearEnrollment(
                            student.getId(), academicYear.getId(), schoolClass.getId(),
                            EnrollmentStatus.ACTIVE, ENROLLED_AT));
            enrollment.setCurrentClassId(schoolClass.getId());
            enrollment.setEnrolledAt(ENROLLED_AT);
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setCompletedAt(null);
            result.put(student.getStudentCode(), enrollmentRepository.save(enrollment));
        }
        return result;
    }

    private boolean seedScorebooksAndScores(
            Student student,
            StudentYearEnrollment enrollment,
            List<SchoolClass> classes,
            List<Semester> semesters,
            Map<String, List<ClassSubject>> classSubjectsByKey,
            Long actorId) {
        String classCode = classes.stream()
                .filter(item -> Objects.equals(item.getId(), enrollment.getCurrentClassId()))
                .map(SchoolClass::getClassCode)
                .findFirst()
                .orElseThrow();
        boolean changed = false;
        int studentIndex = studentNumber(student.getStudentCode()) - FIRST_STUDENT_NUMBER;
        for (int semesterIndex = 0; semesterIndex < semesters.size(); semesterIndex++) {
            String key = classCode + "|" + semesters.get(semesterIndex).getCode();
            List<ClassSubject> classSubjects = classSubjectsByKey.getOrDefault(key, List.of());
            for (ClassSubject classSubject : classSubjects) {
                Scorebook scorebook = scorebookRepository.findByClassSubjectId(classSubject.getId())
                        .orElseGet(() -> scorebookRepository.save(
                                new Scorebook(classSubject.getId(), ScorebookStatus.CLOSED)));
                List<AssessmentColumn> columns = ensureColumns(scorebook);
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    AssessmentColumn column = columns.get(columnIndex);
                    BigDecimal value = scoreFor(studentIndex, semesterIndex, classSubject.getSubjectId(), columnIndex);
                    StudentScore score = scoreRepository
                            .findByAssessmentColumnIdAndStudentId(column.getId(), student.getId())
                            .orElse(null);
                    if (score == null) {
                        scoreRepository.save(new StudentScore(
                                column.getId(), student.getId(), ScoreStatus.SCORED, value, null, actorId));
                        changed = true;
                    } else if (score.getScoreStatus() != ScoreStatus.SCORED
                            || !Objects.equals(score.getScoreValue(), value)) {
                        score.updateScore(ScoreStatus.SCORED, value, null, actorId);
                        scoreRepository.save(score);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private List<AssessmentColumn> ensureColumns(Scorebook scorebook) {
        List<AssessmentColumn> existing = columnRepository
                .findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(scorebook.getId());
        List<AssessmentColumn> columns = new ArrayList<>();
        for (ColumnSeed seed : COLUMN_SEEDS) {
            AssessmentColumn column = existing.stream()
                    .filter(item -> item.getAssessmentType() == seed.type())
                    .filter(item -> seed.columnNo() == item.getColumnNo())
                    .findFirst()
                    .orElseGet(() -> columnRepository.save(new AssessmentColumn(
                            scorebook.getId(), seed.type(), seed.columnNo(), seed.name(),
                            new BigDecimal(seed.weight()), true)));
            column.setColumnName(seed.name());
            column.setWeightFactor(new BigDecimal(seed.weight()));
            column.setRequired(true);
            column.setStatus(AssessmentColumnStatus.ACTIVE);
            columns.add(columnRepository.save(column));
        }
        scorebook.setStatus(ScorebookStatus.CLOSED);
        scorebookRepository.save(scorebook);
        return columns;
    }

    private void ensureTranscript(
            Student student,
            StudentYearEnrollment enrollment,
            AcademicYear academicYear,
            List<Semester> semesters,
            boolean scoreDataChanged) {
        StudentAnnualTranscript annual = annualTranscriptRepository
                .findByStudentIdAndAcademicYearId(student.getId(), academicYear.getId())
                .orElse(null);
        if (!scoreDataChanged && annual != null && isComplete(annual, semesters)) {
            return;
        }
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setCompletedAt(null);
        enrollmentRepository.save(enrollment);
        long sourceVersion = transcriptStateService.touchAnnualTranscript(student.getId(), academicYear.getId());
        recalculationService.recalculate(student.getId(), academicYear.getId(), sourceVersion, null);
    }

    private boolean isComplete(StudentAnnualTranscript annual, List<Semester> semesters) {
        if (annual.getCalculationStatus() != CalculationStatus.FINISH
                || !Objects.equals(annual.getSourceVersion(), annual.getCalculatedVersion())
                || annual.getFinalDtbcn() == null
                || annualResultRepository.findAllByAnnualTranscriptIdOrderBySubjectIdAsc(annual.getId()).size()
                        != SUBJECT_CODES.size()) {
            return false;
        }
        List<StudentSubjectAnnualResult> annualResults = annualResultRepository
                .findAllByAnnualTranscriptIdOrderBySubjectIdAsc(annual.getId());
        if (annualResults.stream().anyMatch(result -> result.getOfficialDtbmhCn() == null
                || result.getHk1TermResultId() == null || result.getHk2TermResultId() == null)) {
            return false;
        }
        for (Semester semester : semesters) {
            StudentTermTranscript term = termTranscriptRepository
                    .findByAnnualTranscriptIdAndSemesterId(annual.getId(), semester.getId())
                    .orElse(null);
            if (term == null || term.getCalculationStatus() != CalculationStatus.FINISH
                    || !Objects.equals(term.getSourceVersion(), term.getCalculatedVersion())
                    || term.getDtbhk() == null
                    || termResultRepository.findAllByTermTranscriptIdOrderBySubjectIdAsc(term.getId()).size()
                            != SUBJECT_CODES.size()) {
                return false;
            }
            List<StudentSubjectTermResult> termResults = termResultRepository
                    .findAllByTermTranscriptIdOrderBySubjectIdAsc(term.getId());
            if (termResults.stream().anyMatch(result -> result.getDtbmh() == null)) {
                return false;
            }
        }
        return true;
    }

    private void completeEnrollment(StudentYearEnrollment enrollment) {
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setCompletedAt(COMPLETED_AT);
        enrollmentRepository.save(enrollment);
    }

    private BigDecimal scoreFor(int studentIndex, int semesterIndex, Long subjectId, int columnIndex) {
        int offset = Math.floorMod(studentIndex * 11 + semesterIndex * 7
                + subjectId.intValue() * 3 + columnIndex * 5, 31);
        return BigDecimal.valueOf(65L + offset, 1);
    }

    private boolean isTargetStudent(String code) {
        return code != null && code.matches(STUDENT_CODE_PREFIX + "\\d{4}")
                && studentNumber(code) >= FIRST_STUDENT_NUMBER
                && studentNumber(code) <= LAST_STUDENT_NUMBER;
    }

    private int studentNumber(String code) {
        return Integer.parseInt(code.substring(STUDENT_CODE_PREFIX.length()));
    }

    private record ColumnSeed(AssessmentType type, int columnNo, String name, String weight) {
    }
}
