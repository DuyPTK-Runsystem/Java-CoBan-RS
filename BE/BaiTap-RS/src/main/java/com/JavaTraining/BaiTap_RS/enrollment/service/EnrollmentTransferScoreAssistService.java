package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResTransferClassDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResTransferScoreAssistDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResTransferScoreSubjectDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResTransferSourceScoreDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResTransferTargetColumnDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentTransferScoreAssistService {

    public ResTransferScoreAssistDTO toAssist(TransferScoreContext context, TransferScoreData data) {
        Map<Long, List<AssessmentColumn>> sourceColumnsBySubject = columnsBySubject(
                data.sourceColumns(), data.sourceColumnSubjects());
        Map<Long, List<AssessmentColumn>> targetColumnsBySubject = columnsBySubject(
                data.targetColumns(), data.targetColumnSubjects());
        Set<Long> subjectIds = new HashSet<>(sourceColumnsBySubject.keySet());
        subjectIds.addAll(targetColumnsBySubject.keySet());
        List<ResTransferScoreSubjectDTO> subjects = subjectIds.stream().sorted().map(subjectId ->
                toSubject(subjectId, sourceColumnsBySubject, targetColumnsBySubject, data)).toList();
        boolean hasScores = !data.sourceScores().isEmpty();
        List<String> warnings = warnings(sourceColumnsBySubject, targetColumnsBySubject, hasScores);
        return new ResTransferScoreAssistDTO(
                context.enrollment().getId(), context.student().getId(), context.student().getStudentCode(),
                context.student().getStudentName(), context.year().getId(), context.semester().getId(), hasScores,
                toClass(context.sourceClass()), toClass(context.targetClass()), subjects, warnings);
    }

    private ResTransferScoreSubjectDTO toSubject(
            Long subjectId,
            Map<Long, List<AssessmentColumn>> sourceBySubject,
            Map<Long, List<AssessmentColumn>> targetBySubject,
            TransferScoreData data) {
        List<AssessmentColumn> source = sourceBySubject.getOrDefault(subjectId, List.of());
        List<AssessmentColumn> target = targetBySubject.getOrDefault(subjectId, List.of());
        Map<String, AssessmentColumn> sourceByKey = source.stream()
                .collect(Collectors.toMap(column -> mappingKey(subjectId, column), column -> column));
        com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject subject = data.subjects().get(subjectId);
        return new ResTransferScoreSubjectDTO(
                subjectId,
                subject == null ? null : subject.getCode(),
                subject == null ? null : subject.getName(),
                source.stream().map(column -> sourceScore(column, data)).toList(),
                target.stream().map(column -> targetColumn(subjectId, column, sourceByKey, data)).toList());
    }

    private List<String> warnings(
            Map<Long, List<AssessmentColumn>> sourceBySubject,
            Map<Long, List<AssessmentColumn>> targetBySubject,
            boolean hasScores) {
        List<String> warnings = new ArrayList<>();
        if (!hasScores) {
            warnings.add("Học sinh chưa có score entry trong học kỳ này");
        }
        targetBySubject.forEach((subjectId, columns) -> {
            Set<String> sourceKeys = sourceBySubject.getOrDefault(subjectId, List.of()).stream()
                    .map(column -> mappingKey(subjectId, column)).collect(Collectors.toSet());
            columns.stream().filter(column -> !sourceKeys.contains(mappingKey(subjectId, column)))
                    .forEach(column -> warnings.add("Cột đích chưa ghép được với bằng chứng cũ: " + column.getId()));
        });
        return warnings;
    }

    private ResTransferSourceScoreDTO sourceScore(AssessmentColumn column, TransferScoreData data) {
        StudentScore score = data.sourceScores().get(column.getId());
        return new ResTransferSourceScoreDTO(column.getId(), column.getScorebookId(), column.getAssessmentType(),
                column.getColumnNo(), column.getColumnName(), score == null ? null : score.getScoreStatus(),
                score == null ? null : score.getScoreValue(), score == null ? null : score.getNote(),
                score == null ? null : score.getVersion());
    }

    private ResTransferTargetColumnDTO targetColumn(
            Long subjectId, AssessmentColumn column, Map<String, AssessmentColumn> sourceByKey,
            TransferScoreData data) {
        AssessmentColumn source = sourceByKey.get(mappingKey(subjectId, column));
        StudentScore existing = data.targetScores().get(column.getId());
        return new ResTransferTargetColumnDTO(column.getId(), column.getScorebookId(), column.getAssessmentType(),
                column.getColumnNo(), column.getColumnName(), column.getStatus(), mappingKey(subjectId, column),
                source == null ? null : source.getId(), existing == null ? null : existing.getScoreStatus(),
                existing == null ? null : existing.getScoreValue(), existing == null ? null : existing.getNote(),
                existing == null ? null : existing.getVersion());
    }

    private Map<Long, List<AssessmentColumn>> columnsBySubject(
            List<AssessmentColumn> columns, Map<Long, Long> columnSubjects) {
        return columns.stream().collect(Collectors.groupingBy(
                column -> findSubjectId(column, columnSubjects), LinkedHashMap::new, Collectors.toList()));
    }

    private Long findSubjectId(AssessmentColumn column, Map<Long, Long> columnSubjects) {
        return columnSubjects.entrySet().stream().filter(entry -> entry.getKey().equals(column.getId()))
                .map(Map.Entry::getValue).findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, "Không xác định được môn của cột điểm"));
    }

    private String mappingKey(Long subjectId, AssessmentColumn column) {
        return subjectId + ":" + column.getAssessmentType() + ":" + column.getColumnNo();
    }

    private ResTransferClassDTO toClass(SchoolClass schoolClass) {
        return new ResTransferClassDTO(schoolClass.getId(), schoolClass.getAcademicYearId(),
                schoolClass.getClassCode(), schoolClass.getClassName());
    }
}
