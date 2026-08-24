package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreGridColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreGridStudentRowDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreGridDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Helper tải và ánh xạ dữ liệu bảng điểm học sinh.
 */
@Component
public class ScoreGridLoader {

        private final StudentScoreRepository scoreRepository;
        private final AssessmentColumnRepository columnRepository;
        private final EnrollmentRosterService rosterService;
        private final ScoreResponseMapper responseMapper;

        public ScoreGridLoader(
                        StudentScoreRepository scoreRepository,
                        AssessmentColumnRepository columnRepository,
                        EnrollmentRosterService rosterService,
                        ScoreResponseMapper responseMapper) {
                this.scoreRepository = scoreRepository;
                this.columnRepository = columnRepository;
                this.rosterService = rosterService;
                this.responseMapper = responseMapper;
        }

        public ResStudentScoreGridDTO loadGrid(
                        Scorebook scorebook, Long classId, Long semesterId, Pageable pageable) {

                List<AssessmentColumn> columns = columnRepository
                                .findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(scorebook.getId())
                                .stream()
                                .filter(c -> c.getStatus() == AssessmentColumnStatus.ACTIVE)
                                .toList();

                List<Long> columnIds = columns.stream().map(AssessmentColumn::getId).toList();
                List<ResScoreGridColumnDTO> gridColumns = columns.stream()
                                .map(c -> new ResScoreGridColumnDTO(
                                                c.getId(), c.getAssessmentType(), c.getColumnNo(), c.getColumnName()))
                                .toList();

                Page<StudentYearEnrollment> enrollmentPage = rosterService
                                .findActiveRoster(classId, semesterId, pageable);

                List<Long> studentIds = enrollmentPage.getContent().stream()
                                .map(StudentYearEnrollment::getStudentId)
                                .toList();

                Map<Long, Student> studentMap = rosterService.loadStudents(studentIds);
                Map<String, StudentScore> scoreMap = loadScoreMap(columnIds, studentIds);

                List<ResScoreGridStudentRowDTO> rows = enrollmentPage.getContent().stream()
                                .map(enrollment -> buildRow(enrollment, studentMap, columnIds, scoreMap))
                                .filter(Objects::nonNull)
                                .toList();

                return new ResStudentScoreGridDTO(
                                scorebook.getId(),
                                scorebook.getClassSubjectId(),
                                scorebook.getStatus(),
                                gridColumns,
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                enrollmentPage.getTotalElements(),
                                enrollmentPage.getTotalPages(),
                                rows);
        }

        private ResScoreGridStudentRowDTO buildRow(
                        StudentYearEnrollment enrollment,
                        Map<Long, Student> studentMap,
                        List<Long> columnIds,
                        Map<String, StudentScore> scoreMap) {
                Long studentId = enrollment.getStudentId();
                Student student = studentMap.get(studentId);
                if (student == null) {
                        return null;
                }

                Map<Long, ResStudentScoreDTO> scoresByColumn = new HashMap<>();
                for (Long colId : columnIds) {
                        String key = colId + ":" + studentId;
                        StudentScore score = scoreMap.get(key);
                        scoresByColumn.put(colId, score != null ? responseMapper.toResponse(score) : null);
                }

                return new ResScoreGridStudentRowDTO(
                                studentId, student.getStudentCode(), student.getStudentName(), scoresByColumn);
        }

        private Map<String, StudentScore> loadScoreMap(List<Long> columnIds, List<Long> studentIds) {
                if (columnIds.isEmpty() || studentIds.isEmpty()) {
                        return Map.of();
                }
                return scoreRepository
                                .findAllByAssessmentColumnIdInAndStudentIdIn(columnIds, studentIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                s -> s.getAssessmentColumnId() + ":" + s.getStudentId(),
                                                s -> s));
        }
}
