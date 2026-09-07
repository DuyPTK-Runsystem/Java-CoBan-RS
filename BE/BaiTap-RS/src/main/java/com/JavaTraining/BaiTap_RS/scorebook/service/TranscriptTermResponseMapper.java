package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TranscriptTermResponseMapper {

    private final SubjectRepository subjectRepository;
    private final ScorebookRepository scorebookRepository;
    private final TranscriptAssessmentColumnMapper assessmentColumnMapper;

    public List<ResStudentTermTranscriptDTO.ResTermSubjectResultDTO> map(
            Long studentId, List<StudentSubjectTermResult> results, Map<Long, ClassSubject> classSubjects) {
        Map<Long, Subject> subjects = subjectRepository.findAllById(results.stream()
                .map(StudentSubjectTermResult::getSubjectId).toList()).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
        Map<Long, Scorebook> scorebooks = classSubjects.isEmpty() ? Map.of()
                : scorebookRepository.findAllByClassSubjectIdIn(classSubjects.keySet()).stream()
                        .collect(Collectors.toMap(Scorebook::getClassSubjectId, Function.identity()));
        Map<Long, List<ResStudentTermTranscriptDTO.ResAssessmentColumnDTO>> columns = assessmentColumnMapper
                .map(studentId, scorebooks);
        return results.stream().map(result -> new ResStudentTermTranscriptDTO.ResTermSubjectResultDTO(
                result.getSubjectId(), subjects.get(result.getSubjectId()).getName(), result.getSubjectType(),
                result.getDtbmh(), result.getSkillScore(), result.getCalculatedVersion(), result.getCalculatedAt(),
                columns.getOrDefault(result.getClassSubjectId(), List.of()))).toList();
    }
}
