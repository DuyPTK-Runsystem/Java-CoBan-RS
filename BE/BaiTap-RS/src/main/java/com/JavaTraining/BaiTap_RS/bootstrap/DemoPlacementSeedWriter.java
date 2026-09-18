package com.JavaTraining.BaiTap_RS.bootstrap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementIssueSeverity;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResultStatus;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSourceType;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementCandidateRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementResultRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;

final class DemoPlacementSeedWriter {

    private static final String CLASS_6A1 = "6A1";
    private static final String CLASS_6A2 = "6A2";
    private static final String CLASS_7A1 = "7A1";
    private static final String GENDER_MALE = "MALE";
    private static final String GENDER_FEMALE = "FEMALE";
    private static final List<CandidateSeed> CANDIDATES = List.of(
            new CandidateSeed("STU2600001", CLASS_6A1, "9.5", GENDER_MALE, null),
            new CandidateSeed("STU2600002", CLASS_6A1, "8.8", GENDER_FEMALE, null),
            new CandidateSeed("STU2600003", CLASS_6A1, "8.8", GENDER_MALE, null),
            new CandidateSeed("STU2600004", CLASS_6A1, "7.0", GENDER_FEMALE, null),
            new CandidateSeed("STU2600005", CLASS_6A2, null, GENDER_MALE, "MISSING_SCORE"),
            new CandidateSeed("STU2600006", CLASS_6A2, "6.5", null, "MISSING_GENDER"),
            new CandidateSeed("STU2600007", CLASS_6A2, "5.0", GENDER_FEMALE, null),
            new CandidateSeed("STU2600008", CLASS_6A2, "4.5", GENDER_MALE, null),
            new CandidateSeed("STU2600009", CLASS_7A1, "9.0", GENDER_FEMALE, null),
            new CandidateSeed("STU2600010", CLASS_7A1, "9.0", GENDER_MALE, null),
            new CandidateSeed("STU2600011", CLASS_7A1, "7.5", GENDER_FEMALE, "MANUAL_REVIEW"),
            new CandidateSeed("STU2600012", CLASS_7A1, "7.2", GENDER_MALE, "ENROLLMENT_CONFLICT"));

    private final PlacementCandidateRepository candidateRepository;
    private final PlacementResultRepository resultRepository;

    /* default */
    DemoPlacementSeedWriter(
            PlacementCandidateRepository candidateRepository,
            PlacementResultRepository resultRepository) {
        this.candidateRepository = candidateRepository;
        this.resultRepository = resultRepository;
    }

    /* default */
    List<String> studentCodes() {
        return CANDIDATES.stream().map(CandidateSeed::studentCode).toList();
    }

    /* default */
    void seedRows(
            PlacementSession session,
            GradeLevel targetGrade,
            List<SchoolClass> targetClasses,
            Map<String, Student> students) {
        Map<Long, PlacementCandidate> existingCandidates = candidateRepository
                .findAllBySessionIdOrderByStudentIdAsc(session.getId()).stream()
                .collect(Collectors.toMap(PlacementCandidate::getStudentId, item -> item));
        Map<Long, PlacementResult> existingResults = resultRepository
                .findAllBySessionIdOrderByStudentIdAsc(session.getId()).stream()
                .collect(Collectors.toMap(PlacementResult::getStudentId, item -> item));
        List<PlacementCandidate> candidates = new ArrayList<>();
        List<PlacementResult> results = new ArrayList<>();
        for (int index = 0; index < CANDIDATES.size(); index++) {
            CandidateSeed seed = CANDIDATES.get(index);
            Student student = students.get(seed.studentCode());
            if (!existingCandidates.containsKey(student.getId())) {
                candidates.add(createCandidate(session, targetGrade, student, seed));
            }
            if (!existingResults.containsKey(student.getId())) {
                results.add(createResult(session, student, targetClasses, index, seed));
            }
        }
        if (!candidates.isEmpty()) {
            candidateRepository.saveAll(candidates);
        }
        if (!results.isEmpty()) {
            resultRepository.saveAll(results);
        }
    }

    private PlacementCandidate createCandidate(
            PlacementSession session,
            GradeLevel targetGrade,
            Student student,
            CandidateSeed seed) {
        return new PlacementCandidate(
                session.getId(), student.getId(), targetGrade.getId(), PlacementSourceType.CONTINUING,
                score(seed.score()), "Plan 081 score snapshot", seed.gender(),
                eligibilityEvidence(seed), null);
    }

    private PlacementResult createResult(
            PlacementSession session,
            Student student,
            List<SchoolClass> targetClasses,
            int index,
            CandidateSeed seed) {
        PlacementResultStatus resultStatus = seed.issueCode() == null
                ? PlacementResultStatus.AUTO_ASSIGNED : PlacementResultStatus.MANUAL_REQUIRED;
        Long targetClassId = resultStatus == PlacementResultStatus.AUTO_ASSIGNED
                ? targetClasses.get(index % targetClasses.size()).getId() : null;
        return new PlacementResult(
                session.getId(), student.getId(), targetClassId, resultStatus, score(seed.score()),
                seed.issueCode(), issueSeverity(seed.issueCode()), explanation(seed.issueCode()));
    }

    private String eligibilityEvidence(CandidateSeed seed) {
        return "{\"sourceClassCode\":\"" + seed.sourceClassCode() + "\",\"scenario\":\""
                + (seed.issueCode() == null ? "FULL_DATA" : seed.issueCode()) + "\"}";
    }

    private BigDecimal score(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private PlacementIssueSeverity issueSeverity(String issueCode) {
        return issueCode == null ? null : PlacementIssueSeverity.WARNING;
    }

    private String explanation(String issueCode) {
        if (issueCode == null) {
            return "Plan 081 demo result: tự động phân lớp theo snapshot điểm và tiêu chí đã lưu.";
        }
        return switch (issueCode) {
            case "MISSING_SCORE" -> "Thiếu score snapshot; cần xử lý thủ công, không tự suy diễn điểm.";
            case "MISSING_GENDER" -> "Thiếu gender snapshot; cần xử lý thủ công, không tự bù giới tính.";
            case "MANUAL_REVIEW" -> "Hồ sơ được đánh dấu để kiểm tra thủ công theo fixture Plan 081.";
            default -> "Có conflict enrollment trong fixture Plan 081; cần xử lý thủ công.";
        };
    }

    private record CandidateSeed(
            String studentCode,
            String sourceClassCode,
            String score,
            String gender,
            String issueCode) {
    }
}
