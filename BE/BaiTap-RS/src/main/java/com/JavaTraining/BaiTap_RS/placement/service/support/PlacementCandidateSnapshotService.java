package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqCreatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSourceType;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementCandidateRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlacementCandidateSnapshotService {
    private final AcademicYearRepository academicYears;
    private final StudentAnnualTranscriptRepository annualTranscripts;
    private final StudentInfoRepository studentInfos;
    private final PlacementCandidateRepository candidates;
    private final PlacementCandidateEligibilityValidator eligibilityValidator;

    public void snapshot(PlacementSession session, List<ReqCreatePlacementSessionDTO.Candidate> requested) {
        AcademicYear targetYear = academicYears.findById(session.getAcademicYearId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học đích"));
        validateSources(targetYear, session.getTargetGradeId(), requested);
        Map<Long, OfficialScore> officialScores = officialScores(targetYear, requested);
        requested.forEach(candidate -> candidates.save(
                toSnapshot(session, candidate, officialScores.get(candidate.studentId()))));
    }

    private PlacementCandidate toSnapshot(PlacementSession session, ReqCreatePlacementSessionDTO.Candidate candidate,
            OfficialScore officialScore) {
        String gender = studentInfos.findByStudentId(candidate.studentId())
                .map(info -> info.getGender() == null ? null : info.getGender().name()).orElse(null);
        return new PlacementCandidate(session.getId(), candidate.studentId(), candidate.targetGradeId(),
                candidate.sourceType(),
                officialScore == null ? null : officialScore.value(),
                officialScore == null ? null : officialScore.reference(), gender,
                candidate.eligibilityEvidence(), candidate.approvalReference());
    }

    private Map<Long, OfficialScore> officialScores(AcademicYear targetYear,
            List<ReqCreatePlacementSessionDTO.Candidate> requested) {
        Optional<AcademicYear> previous = previousYear(targetYear);
        if (previous.isEmpty()) {
            return Map.of();
        }
        List<Long> studentIds = requested.stream()
                .filter(candidate -> candidate.sourceType() != PlacementSourceType.NEW_ADMISSION)
                .map(ReqCreatePlacementSessionDTO.Candidate::studentId).toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return annualTranscripts.findAllByAcademicYearIdAndStudentIdIn(previous.get().getId(), studentIds).stream()
                .filter(this::isOfficial).collect(Collectors.toMap(StudentAnnualTranscript::getStudentId,
                        transcript -> new OfficialScore(transcript.getFinalDtbcn(), "student_annual_transcript:"
                                + transcript.getId() + ":finalDtbcn:v" + transcript.getCalculatedVersion())));
    }

    private void validateSources(AcademicYear targetYear, Long targetGradeId,
            List<ReqCreatePlacementSessionDTO.Candidate> requested) {
        Optional<AcademicYear> previous = previousYear(targetYear);
        for (ReqCreatePlacementSessionDTO.Candidate candidate : requested) {
            eligibilityValidator.validate(candidate, previous, targetGradeId);
        }
    }

    private Optional<AcademicYear> previousYear(AcademicYear targetYear) {
        return academicYears.findTopByEndDateLessThanOrderByEndDateDesc(targetYear.getStartDate());
    }

    private boolean isOfficial(StudentAnnualTranscript transcript) {
        return transcript.getCalculationStatus() == CalculationStatus.FINISH && transcript.getFinalDtbcn() != null
                && transcript.getSourceVersion() != null && transcript.getCalculatedVersion() != null
                && transcript.getSourceVersion().equals(transcript.getCalculatedVersion());
    }

    private record OfficialScore(BigDecimal value, String reference) { }
}
