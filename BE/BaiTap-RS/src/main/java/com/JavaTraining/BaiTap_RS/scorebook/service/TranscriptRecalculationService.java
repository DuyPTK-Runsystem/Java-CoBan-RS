package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationResultSource;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.SkillWeightConfigRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalculates one student's complete regular transcript in dependency order.
 */
@Service
@SuppressWarnings({
        "PMD.CouplingBetweenObjects",
        "PMD.ExcessiveImports",
        "PMD.ExcessiveParameterList",
        "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.CognitiveComplexity",
        "PMD.CyclomaticComplexity",
        "PMD.NPathComplexity"
})
public class TranscriptRecalculationService {

    private final SubjectScoreCalculator calculator;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final SemesterRepository semesterRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final ScorebookRepository scorebookRepository;
    private final AssessmentColumnRepository columnRepository;
    private final StudentScoreRepository scoreRepository;
    private final SkillWeightConfigRepository skillWeightRepository;
    private final StudentAnnualTranscriptRepository annualTranscriptRepository;
    private final StudentTermTranscriptRepository termTranscriptRepository;
    private final StudentSubjectTermResultRepository termResultRepository;
    private final StudentSubjectAnnualResultRepository annualResultRepository;

    public TranscriptRecalculationService(
            SubjectScoreCalculator calculator,
            StudentYearEnrollmentRepository enrollmentRepository,
            SemesterRepository semesterRepository,
            ClassSubjectRepository classSubjectRepository,
            SubjectRepository subjectRepository,
            ScorebookRepository scorebookRepository,
            AssessmentColumnRepository columnRepository,
            StudentScoreRepository scoreRepository,
            SkillWeightConfigRepository skillWeightRepository,
            StudentAnnualTranscriptRepository annualTranscriptRepository,
            StudentTermTranscriptRepository termTranscriptRepository,
            StudentSubjectTermResultRepository termResultRepository,
            StudentSubjectAnnualResultRepository annualResultRepository) {
        this.calculator = calculator;
        this.enrollmentRepository = enrollmentRepository;
        this.semesterRepository = semesterRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.scorebookRepository = scorebookRepository;
        this.columnRepository = columnRepository;
        this.scoreRepository = scoreRepository;
        this.skillWeightRepository = skillWeightRepository;
        this.annualTranscriptRepository = annualTranscriptRepository;
        this.termTranscriptRepository = termTranscriptRepository;
        this.termResultRepository = termResultRepository;
        this.annualResultRepository = annualResultRepository;
    }

    @Transactional
    public void recalculate(Long studentId, Long academicYearId, Long requestedVersion, Long taskId) {
        StudentYearEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .filter(item -> item.getStatus() == EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new AppException(
                        HttpStatus.CONFLICT, "Học sinh chưa có enrollment ACTIVE trong năm học"));
        StudentAnnualTranscript annualTranscript = annualTranscriptRepository
                .findForUpdate(studentId, academicYearId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy bảng điểm tổng kết của học sinh"));

        long currentSourceVersion = annualTranscript.getSourceVersion();
        List<Semester> semesters = semesterRepository
                .findAllByAcademicYearIdOrderByEndDateAscDisplayOrderAscIdAsc(academicYearId);
        Map<Long, List<TermResultRef>> resultsBySubject = new LinkedHashMap<>();

        for (Semester semester : semesters) {
            calculateTerm(
                    enrollment,
                    semester,
                    annualTranscript,
                    requestedVersion,
                    currentSourceVersion,
                    resultsBySubject);
        }

        calculateAnnualResults(
                annualTranscript,
                semesters,
                resultsBySubject,
                requestedVersion,
                currentSourceVersion,
                taskId);
    }

    private void calculateTerm(
            StudentYearEnrollment enrollment,
            Semester semester,
            StudentAnnualTranscript annualTranscript,
            Long requestedVersion,
            long currentSourceVersion,
            Map<Long, List<TermResultRef>> resultsBySubject) {
        StudentTermTranscript termTranscript = termTranscriptRepository
                .findByAnnualTranscriptIdAndSemesterId(annualTranscript.getId(), semester.getId())
                .orElseGet(() -> new StudentTermTranscript(
                        annualTranscript.getId(), semester.getId(), enrollment.getStudentId()));
        termTranscript.setSourceVersion(currentSourceVersion);
        termTranscript = termTranscriptRepository.save(termTranscript);

        List<BigDecimal> normalScores = new ArrayList<>();
        List<ClassSubject> classSubjects = classSubjectRepository
                .findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(enrollment.getCurrentClassId(), semester.getId())
                .stream()
                .filter(classSubject -> classSubject.getStatus() != ClassSubjectStatus.INACTIVE)
                .toList();
        for (ClassSubject classSubject : classSubjects) {
            Subject subject = findSubject(classSubject.getSubjectId());
            StudentSubjectTermResult result = calculateSubjectResult(
                    classSubject, subject, enrollment.getStudentId(), termTranscript.getId());
            termResultRepository.save(result);
            resultsBySubject.computeIfAbsent(subject.getId(), ignored -> new ArrayList<>())
                    .add(new TermResultRef(semester.getId(), subject.getSubjectType(), result));
            if (subject.getSubjectType() == SubjectType.ACADEMIC && result.getDtbmh() != null) {
                normalScores.add(result.getDtbmh());
            }
        }

        termTranscript.setDtbhk(calculator.calculateTermAverage(normalScores));
        termTranscript.setCalculatedVersion(requestedVersion);
        termTranscript.setCalculatedAt(LocalDateTime.now());
        termTranscript.setCalculationStatus(
                currentSourceVersion == requestedVersion ? CalculationStatus.FINISH : CalculationStatus.IN_PROGRESS);
        termTranscriptRepository.save(termTranscript);
    }

    private StudentSubjectTermResult calculateSubjectResult(
            ClassSubject classSubject,
            Subject subject,
            Long studentId,
            Long termTranscriptId) {
        StudentSubjectTermResult result = termResultRepository
                .findByTermTranscriptIdAndSubjectId(termTranscriptId, subject.getId())
                .orElseGet(() -> new StudentSubjectTermResult(
                        termTranscriptId, classSubject.getId(), subject.getId(), subject.getSubjectType()));
        result.setClassSubjectId(classSubject.getId());
        result.setSubjectType(subject.getSubjectType());
        result.setDtbmh(null);
        result.setSkillScore(null);

        Scorebook scorebook = scorebookRepository.findByClassSubjectId(classSubject.getId()).orElse(null);
        if (scorebook != null) {
            List<AssessmentColumn> columns = columnRepository
                    .findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(scorebook.getId())
                    .stream()
                    .filter(column -> column.getStatus() == AssessmentColumnStatus.ACTIVE)
                    .toList();
            List<StudentScore> scores = loadScores(columns, studentId);
            if (subject.getSubjectType() == SubjectType.ACADEMIC) {
                result.setDtbmh(calculator.calculateNormalSubjectTermScore(columns, scores));
            } else {
                SkillWeightConfig config = skillWeightRepository.findByScorebookId(scorebook.getId()).orElse(null);
                result.setSkillScore(calculator.calculateSkillSubjectTermScore(config, columns, scores));
            }
        }
        result.setCalculatedAt(LocalDateTime.now());
        return result;
    }

    private void calculateAnnualResults(
            StudentAnnualTranscript annualTranscript,
            List<Semester> semesters,
            Map<Long, List<TermResultRef>> resultsBySubject,
            Long requestedVersion,
            long currentSourceVersion,
            Long taskId) {
        Long firstSemesterId = semesters.isEmpty() ? null : semesters.get(0).getId();
        Long secondSemesterId = semesters.size() < 2 ? null : semesters.get(1).getId();
        List<BigDecimal> annualScores = new ArrayList<>();

        for (Map.Entry<Long, List<TermResultRef>> entry : resultsBySubject.entrySet()) {
            Long subjectId = entry.getKey();
            Map<Long, TermResultRef> bySemester = new HashMap<>();
            entry.getValue().forEach(value -> bySemester.put(value.semesterId(), value));
            TermResultRef first = firstSemesterId == null ? null : bySemester.get(firstSemesterId);
            TermResultRef second = secondSemesterId == null ? null : bySemester.get(secondSemesterId);
            SubjectType type = first != null ? first.subjectType() : second.subjectType();
            BigDecimal regularScore = type == SubjectType.ACADEMIC
                    ? calculator.calculateAnnualSubjectScore(
                            score(first), score(second), first != null && second != null)
                    : null;
            StudentSubjectAnnualResult annualResult = annualResultRepository
                    .findByAnnualTranscriptIdAndSubjectId(annualTranscript.getId(), subjectId)
                    .orElseGet(() -> new StudentSubjectAnnualResult(
                            annualTranscript.getId(), subjectId, type));
            annualResult.setSubjectType(type);
            annualResult.setHk1TermResultId(first == null ? null : first.result().getId());
            annualResult.setHk2TermResultId(second == null ? null : second.result().getId());
            annualResult.setRegularDtbmhCn(regularScore);
            annualResult.setOfficialDtbmhCn(regularScore);
            annualResult.setCalculationSource(CalculationResultSource.REGULAR);
            annualResult.setCalculatedVersion(requestedVersion);
            annualResult.setCalculatedAt(LocalDateTime.now());
            annualResultRepository.save(annualResult);
            if (type == SubjectType.ACADEMIC && regularScore != null) {
                annualScores.add(regularScore);
            }
        }

        BigDecimal annualAverage = calculator.calculateAnnualAverage(annualScores);
        annualTranscript.setRegularDtbcn(annualAverage);
        annualTranscript.setFinalDtbcn(annualAverage);
        annualTranscript.setResultSource(CalculationResultSource.REGULAR);
        annualTranscript.setCalculatedVersion(requestedVersion);
        annualTranscript.setCalculatedAt(LocalDateTime.now());
        annualTranscript.setLastCalculationTaskId(taskId);
        annualTranscript.setCalculationStatus(
                currentSourceVersion == requestedVersion ? CalculationStatus.FINISH : CalculationStatus.IN_PROGRESS);
        if (currentSourceVersion == requestedVersion) {
            annualTranscript.setLastError(null);
        }
        annualTranscriptRepository.save(annualTranscript);
    }

    private List<StudentScore> loadScores(List<AssessmentColumn> columns, Long studentId) {
        List<Long> columnIds = columns.stream().map(AssessmentColumn::getId).toList();
        if (columnIds.isEmpty()) {
            return List.of();
        }
        return scoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(columnIds, List.of(studentId));
    }

    private Subject findSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học"));
    }

    private BigDecimal score(TermResultRef reference) {
        return reference == null ? null : reference.result().getDtbmh();
    }

    private record TermResultRef(
            Long semesterId,
            SubjectType subjectType,
            StudentSubjectTermResult result) {
    }
}
