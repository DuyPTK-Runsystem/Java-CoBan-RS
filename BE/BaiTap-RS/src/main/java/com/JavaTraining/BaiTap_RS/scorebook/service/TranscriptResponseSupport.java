package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.ClassTransferHistory;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.ClassTransferHistoryRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExam;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.repository.RetakeExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TranscriptResponseSupport {

    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final RetakeExamRepository retakeExamRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final ClassTransferHistoryRepository transferHistoryRepository;

    public Map<Long, ClassSubject> findClassSubjects(List<Long> ids) {
        return classSubjectRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ClassSubject::getId, Function.identity()));
    }

    public List<ResStudentTermTranscriptDTO.ResTransferNoteDTO> findTransferNotes(Long studentId, Long academicYearId) {
        return enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .map(StudentYearEnrollment::getId)
                .map(transferHistoryRepository::findByEnrollmentIdOrderByEffectiveAtAsc)
                .orElseGet(List::of).stream().sorted(Comparator.comparing(ClassTransferHistory::getEffectiveAt))
                .map(history -> new ResStudentTermTranscriptDTO.ResTransferNoteDTO(history.getFromClassId(),
                        history.getToClassId(), history.getEffectiveAt())).toList();
    }

    public List<ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO> mapAnnualResults(
            List<StudentSubjectAnnualResult> annualResults, Map<Long, StudentSubjectTermResult> termResults) {
        Map<Long, Subject> subjects = subjectRepository.findAllById(annualResults.stream()
                .map(StudentSubjectAnnualResult::getSubjectId).toList()).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
        Map<Long, RetakeExam> retakes = retakeExamRepository.findAllById(annualResults.stream()
                .map(StudentSubjectAnnualResult::getRetakeId).filter(java.util.Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(RetakeExam::getId, Function.identity()));
        return annualResults.stream().map(result -> mapAnnualResult(result, subjects, termResults, retakes)).toList();
    }

    private ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO mapAnnualResult(
            StudentSubjectAnnualResult result, Map<Long, Subject> subjects,
            Map<Long, StudentSubjectTermResult> termResults, Map<Long, RetakeExam> retakes) {
        RetakeExam retake = result.getRetakeId() == null ? null : retakes.get(result.getRetakeId());
        return new ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO(result.getSubjectId(),
                subjects.get(result.getSubjectId()).getName(), result.getSubjectType(),
                scoreOf(termResults.get(result.getHk1TermResultId())),
                scoreOf(termResults.get(result.getHk2TermResultId())), result.getRegularDtbmhCn(),
                result.getOfficialDtbmhCn(), result.getCalculationSource(), result.getCalculatedVersion(),
                result.getCalculatedAt(), retake == null ? null
                        : new ResStudentAnnualTranscriptDTO.ResRetakeDetailDTO(retake.getId(),
                                retake.getPreRetakeScore(), retake.getRetakeScore(), retake.getExamDate(),
                                retake.getStatus(), retake.getNote()));
    }

    private BigDecimal scoreOf(StudentSubjectTermResult result) {
        return result == null ? null
                : result.getSubjectType() == com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType.SKILL
                ? result.getSkillScore() : result.getDtbmh();
    }
}
