package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessReportDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockReport;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockReportStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterLockReportRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScoreChangeRequestRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings({
        "PMD.ExcessiveImports",
        "PMD.AvoidCatchingGenericException",
        "PMD.CouplingBetweenObjects",
        "PMD.CognitiveComplexity",
        "PMD.CyclomaticComplexity",
        "PMD.NPathComplexity",
        "PMD.AvoidDuplicateLiterals"
})
public class SemesterCompletenessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemesterCompletenessService.class);

    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final ScorebookRepository scorebookRepository;
    private final AssessmentColumnRepository assessmentColumnRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final StudentYearEnrollmentRepository studentYearEnrollmentRepository;
    private final ScoreChangeRequestRepository scoreChangeRequestRepository;
    private final SemesterLockReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public SemesterCompletenessService(
            ClassSubjectRepository classSubjectRepository,
            SubjectRepository subjectRepository,
            ScorebookRepository scorebookRepository,
            AssessmentColumnRepository assessmentColumnRepository,
            StudentScoreRepository studentScoreRepository,
            StudentYearEnrollmentRepository studentYearEnrollmentRepository,
            ScoreChangeRequestRepository scoreChangeRequestRepository,
            SemesterLockReportRepository reportRepository,
            ObjectMapper objectMapper) {
        this.classSubjectRepository = classSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.scorebookRepository = scorebookRepository;
        this.assessmentColumnRepository = assessmentColumnRepository;
        this.studentScoreRepository = studentScoreRepository;
        this.studentYearEnrollmentRepository = studentYearEnrollmentRepository;
        this.scoreChangeRequestRepository = scoreChangeRequestRepository;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SemesterCompletenessSummaryDTO evaluateCompleteness(Long semesterId) {
        List<ClassSubject> classSubjects = classSubjectRepository.findAllBySemesterIdAndStatus(
                semesterId, ClassSubjectStatus.ACTIVE);

        int missingKtdkCount = 0;
        int invalidKtckCount = 0;
        int missingSkillColumnsCount = 0;
        int unenteredScoreCount = 0;
        int studentWithoutScoreDataCount = 0;
        int unpublishedScorebookCount = 0;
        List<String> details = new ArrayList<>();
        List<Long> allActiveColumnIds = new ArrayList<>();

        for (ClassSubject classSubject : classSubjects) {
            Optional<Scorebook> scorebookOpt = scorebookRepository.findByClassSubjectId(classSubject.getId());
            if (scorebookOpt.isEmpty() || scorebookOpt.get().getStatus() != ScorebookStatus.PUBLISHED) {
                unpublishedScorebookCount++;
                details.add("ClassSubject " + classSubject.getId() + ": Sổ điểm chưa công bố");
            }

            if (scorebookOpt.isPresent()) {
                Scorebook scorebook = scorebookOpt.get();
                List<AssessmentColumn> columns = assessmentColumnRepository
                        .findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(scorebook.getId())
                        .stream()
                        .filter(col -> col.getStatus() == AssessmentColumnStatus.ACTIVE)
                        .toList();

                for (AssessmentColumn col : columns) {
                    allActiveColumnIds.add(col.getId());
                }

                long ktdkCount = columns.stream()
                        .filter(col -> col.getAssessmentType() == AssessmentType.KTDK)
                        .count();
                if (ktdkCount == 0) {
                    missingKtdkCount++;
                    details.add("ClassSubject " + classSubject.getId() + ": Thiếu cấu hình cột KTĐK");
                }

                long ktckCount = columns.stream()
                        .filter(col -> col.getAssessmentType() == AssessmentType.KTCK)
                        .count();
                if (ktckCount != 1) {
                    details.add("ClassSubject " + classSubject.getId() + ": Không có đúng một cột KTCK (hiện có: "
                            + ktckCount + ")");
                    invalidKtckCount++;
                }

                Subject subject = subjectRepository.findById(classSubject.getSubjectId()).orElse(null);
                if (subject != null && subject.getSubjectType() == SubjectType.SKILL && columns.size() != 3) {
                    missingSkillColumnsCount++;
                    details.add("ClassSubject " + classSubject.getId() + ": Môn kỹ năng thiếu đủ ba cột (hiện có: "
                            + columns.size() + ")");
                }

                List<StudentYearEnrollment> enrollments = studentYearEnrollmentRepository
                        .findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                                classSubject.getClassId(), EnrollmentStatus.ACTIVE);

                List<Long> columnIds = columns.stream().map(AssessmentColumn::getId).toList();
                List<StudentScore> scores = columnIds.isEmpty()
                        ? Collections.emptyList()
                        : studentScoreRepository.findAllByAssessmentColumnIdIn(columnIds);

                Map<Long, List<StudentScore>> scoresByStudent = scores.stream()
                        .collect(Collectors.groupingBy(StudentScore::getStudentId));

                for (StudentYearEnrollment enrollment : enrollments) {
                    Long studentId = enrollment.getStudentId();
                    List<StudentScore> studentScores = scoresByStudent.getOrDefault(studentId, Collections.emptyList());

                    if (studentScores.isEmpty() && !columns.isEmpty()) {
                        studentWithoutScoreDataCount++;
                        details.add("Học sinh " + studentId + " chưa có dữ liệu điểm trong ClassSubject "
                                + classSubject.getId());
                    } else {
                        Set<Long> scoredColumnIds = studentScores.stream()
                                .filter(s -> s.getScoreStatus() != null)
                                .map(StudentScore::getAssessmentColumnId)
                                .collect(Collectors.toSet());

                        for (AssessmentColumn col : columns) {
                            if (col.isRequired() && !scoredColumnIds.contains(col.getId())) {
                                unenteredScoreCount++;
                                details.add("Học sinh " + studentId + " chưa nhập điểm cột " + col.getId()
                                        + " trong ClassSubject " + classSubject.getId());
                            }
                        }
                    }
                }
            }
        }

        int pendingScoreChangeRequestCount = 0;
        if (!allActiveColumnIds.isEmpty()) {
            pendingScoreChangeRequestCount = (int) scoreChangeRequestRepository
                    .countByAssessmentColumnIdInAndStatus(allActiveColumnIds, ScoreChangeRequestStatus.PENDING);
            if (pendingScoreChangeRequestCount > 0) {
                details.add("Có " + pendingScoreChangeRequestCount + " yêu cầu sửa điểm đang PENDING");
            }
        }

        boolean complete = missingKtdkCount == 0
                && invalidKtckCount == 0
                && missingSkillColumnsCount == 0
                && unenteredScoreCount == 0
                && studentWithoutScoreDataCount == 0
                && unpublishedScorebookCount == 0
                && pendingScoreChangeRequestCount == 0;

        return new SemesterCompletenessSummaryDTO(
                complete,
                missingKtdkCount,
                invalidKtckCount,
                missingSkillColumnsCount,
                unenteredScoreCount,
                studentWithoutScoreDataCount,
                unpublishedScorebookCount,
                pendingScoreChangeRequestCount,
                details);
    }

    @Transactional(readOnly = true)
    public ResSemesterCompletenessReportDTO evaluateCompletenessPreview(Long semesterId, String checkpointCode) {
        SemesterCompletenessSummaryDTO summary = evaluateCompleteness(semesterId);
        SemesterLockReportStatus reportStatus = summary.complete()
                ? SemesterLockReportStatus.COMPLETE
                : SemesterLockReportStatus.INCOMPLETE;

        return new ResSemesterCompletenessReportDTO(
                null,
                null,
                semesterId,
                checkpointCode != null ? checkpointCode : "preview",
                reportStatus,
                LocalDateTime.now(),
                "SEMESTER",
                summary,
                null,
                null);
    }

    @Transactional
    public SemesterLockReport evaluateAndSaveReport(
            Long runId,
            Long semesterId,
            String checkpointCode,
            String correlationId) {
        Optional<SemesterLockReport> existing = reportRepository.findByRunIdAndSemesterIdAndCheckpointCode(
                runId, semesterId, checkpointCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            SemesterCompletenessSummaryDTO summary = evaluateCompleteness(semesterId);
            SemesterLockReportStatus reportStatus = summary.complete()
                    ? SemesterLockReportStatus.COMPLETE
                    : SemesterLockReportStatus.INCOMPLETE;
            String payload = objectMapper.writeValueAsString(summary);

            SemesterLockReport report = new SemesterLockReport(
                    runId,
                    semesterId,
                    checkpointCode,
                    reportStatus,
                    LocalDateTime.now(),
                    "SEMESTER",
                    payload,
                    null,
                    correlationId);
            return reportRepository.save(report);
        } catch (Exception exception) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Lỗi khi đánh giá completeness cho semester {}: {}", semesterId, exception.getMessage());
            }
            SemesterLockReport failedReport = new SemesterLockReport(
                    runId,
                    semesterId,
                    checkpointCode,
                    SemesterLockReportStatus.FAILED,
                    LocalDateTime.now(),
                    "SEMESTER",
                    "{}",
                    exception.getMessage(),
                    correlationId);
            return reportRepository.save(failedReport);
        }
    }

    @Transactional(readOnly = true)
    public ResSemesterCompletenessReportDTO getLatestReport(Long semesterId, String checkpointCode) {
        Optional<SemesterLockReport> reportOpt = checkpointCode != null
                ? reportRepository.findFirstBySemesterIdAndCheckpointCodeOrderByEvaluatedAtDesc(
                        semesterId, checkpointCode)
                : reportRepository.findFirstBySemesterIdOrderByEvaluatedAtDesc(semesterId);

        if (reportOpt.isPresent()) {
            SemesterLockReport report = reportOpt.get();
            SemesterCompletenessSummaryDTO summary = parseSummary(report.getSummaryPayload());
            return new ResSemesterCompletenessReportDTO(
                    report.getId(),
                    report.getRunId(),
                    report.getSemesterId(),
                    report.getCheckpointCode(),
                    report.getReportStatus(),
                    report.getEvaluatedAt(),
                    report.getScopeType(),
                    summary,
                    report.getFailureReason(),
                    report.getCorrelationId());
        }

        return evaluateCompletenessPreview(semesterId, checkpointCode);
    }

    private SemesterCompletenessSummaryDTO parseSummary(String payload) {
        try {
            return objectMapper.readValue(payload, SemesterCompletenessSummaryDTO.class);
        } catch (JsonProcessingException exception) {
            return new SemesterCompletenessSummaryDTO(
                    false, 0, 0, 0, 0, 0, 0, 0, List.of("Không thể đọc summary payload: " + exception.getMessage()));
        }
    }
}
