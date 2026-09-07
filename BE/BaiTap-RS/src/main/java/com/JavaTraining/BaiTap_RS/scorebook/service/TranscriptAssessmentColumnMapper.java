package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TranscriptAssessmentColumnMapper {

    private final AssessmentColumnRepository assessmentColumnRepository;
    private final StudentScoreRepository studentScoreRepository;

    public Map<Long, List<ResStudentTermTranscriptDTO.ResAssessmentColumnDTO>> map(
            Long studentId, Map<Long, Scorebook> scorebooks) {
        if (scorebooks.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> scorebookClassSubjects = scorebooks.values().stream()
                .collect(Collectors.toMap(Scorebook::getId, Scorebook::getClassSubjectId));
        List<AssessmentColumn> columns = assessmentColumnRepository
                .findAllByScorebookIdInOrderByScorebookIdAscAssessmentTypeAscColumnNoAsc(
                        scorebookClassSubjects.keySet())
                .stream().filter(column -> column.getStatus() == AssessmentColumnStatus.ACTIVE).toList();
        List<Long> columnIds = columns.stream().map(AssessmentColumn::getId).toList();
        Map<Long, StudentScore> scores = columnIds.isEmpty() ? Map.of()
                : studentScoreRepository.findAllByAssessmentColumnIdInAndStudentIdIn(columnIds, List.of(studentId))
                        .stream()
                .collect(Collectors.toMap(StudentScore::getAssessmentColumnId, Function.identity()));
        return columns.stream().collect(Collectors.groupingBy(
                column -> scorebookClassSubjects.get(column.getScorebookId()),
                Collectors.mapping(column -> new ResStudentTermTranscriptDTO.ResAssessmentColumnDTO(column.getId(),
                        column.getAssessmentType(), column.getColumnNo(), column.getColumnName(),
                        scores.containsKey(column.getId()) ? scores.get(column.getId()).getScoreStatus() : null,
                        scores.containsKey(column.getId()) ? scores.get(column.getId()).getScoreValue() : null),
                        Collectors.toList())));
    }
}
