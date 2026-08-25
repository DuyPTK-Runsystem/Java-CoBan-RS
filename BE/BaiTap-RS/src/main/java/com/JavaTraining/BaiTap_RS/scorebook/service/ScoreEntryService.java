package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqBulkScoreItemDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqBulkUpsertStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service chính điều phối các thao tác nhập và sửa điểm học sinh (Single & Bulk).
 */
@Service
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class ScoreEntryService {

    private final ScoreEntryContext entryContext;
    private final ScoreEntryValidator validator;
    private final ScorebookGuard guard;
    private final TranscriptStateService transcriptService;
    private final CalculationTaskService taskService;
    private final ScoreEntryWriter scoreWriter;
    private final StudentLookupService studentLookupService;

    public ScoreEntryService(
            ScoreEntryContext entryContext,
            ScoreEntryValidator validator,
            ScorebookGuard guard,
            TranscriptStateService transcriptService,
            CalculationTaskService taskService,
            ScoreEntryWriter scoreWriter,
            StudentLookupService studentLookupService) {
        this.entryContext = entryContext;
        this.validator = validator;
        this.guard = guard;
        this.transcriptService = transcriptService;
        this.taskService = taskService;
        this.scoreWriter = scoreWriter;
        this.studentLookupService = studentLookupService;
    }

    @Transactional
    public ResStudentScoreDTO upsertSingleScore(
            Long columnId, Long studentId, ReqUpsertStudentScoreDTO request) {

        AssessmentColumn column = entryContext.findActiveColumn(columnId);
        Scorebook scorebook = entryContext.findWritableScorebook(column.getScorebookId());
        guard.assertCanManage(scorebook);

        ClassSubject classSubject = entryContext.findClassSubject(scorebook.getClassSubjectId());
        Semester semester = entryContext.findSemesterForScoring(classSubject.getSemesterId());

        Student student = entryContext.findActiveStudent(studentId);
        entryContext.validateEnrollment(student.getId(), semester, classSubject.getClassId());
        validator.validateScoreValue(request.scoreStatus(), request.scoreValue());

        Long actorId = AuditContext.currentUserId();
        Optional<StudentScore> existing = scoreWriter.findExisting(columnId, student.getId());

        ResStudentScoreDTO result;
        if (existing.isPresent()) {
            result = scoreWriter.updateExisting(existing.get(), student, request, semester, actorId);
        } else {
            validator.validateCreateVersion(request.expectedVersion());
            result = scoreWriter.createNew(
                    columnId, student, request.scoreStatus(),
                    request.scoreValue(), request.note(), actorId);
        }

        Long academicYearId = semester.getAcademicYearId();
        long newVersion = transcriptService.touchTranscripts(
                student.getId(), academicYearId, classSubject.getSemesterId());
        taskService.ensureRecalcTask(student.getId(), academicYearId, newVersion);

        return result;
    }

    @Transactional
    public List<ResStudentScoreDTO> bulkUpsertScores(
            Long columnId, ReqBulkUpsertStudentScoreDTO request) {

        AssessmentColumn column = entryContext.findActiveColumn(columnId);
        Scorebook scorebook = entryContext.findWritableScorebook(column.getScorebookId());
        guard.assertCanManage(scorebook);

        ClassSubject classSubject = entryContext.findClassSubject(scorebook.getClassSubjectId());
        Semester semester = entryContext.findSemesterForScoring(classSubject.getSemesterId());

        Long actorId = AuditContext.currentUserId();
        Long academicYearId = semester.getAcademicYearId();
        List<ResStudentScoreDTO> results = new ArrayList<>();
        Set<Long> affectedStudentIds = new HashSet<>();
        List<ResolvedBulkScoreItem> resolvedItems = resolveBulkItems(request.items());
        validateNoDuplicateResolvedStudents(resolvedItems);

        for (ResolvedBulkScoreItem resolvedItem : resolvedItems) {
            ResStudentScoreDTO res = processBulkItem(
                    resolvedItem, columnId, classSubject, semester, actorId, affectedStudentIds);
            results.add(res);
        }

        for (Long studentId : affectedStudentIds) {
            long newVersion = transcriptService.touchTranscripts(
                    studentId, academicYearId, classSubject.getSemesterId());
            taskService.ensureRecalcTask(studentId, academicYearId, newVersion);
        }

        return results;
    }

    private ResStudentScoreDTO processBulkItem(
            ResolvedBulkScoreItem resolvedItem,
            Long columnId,
            ClassSubject classSubject,
            Semester semester,
            Long actorId,
            Set<Long> affectedStudentIds) {

        ReqBulkScoreItemDTO item = resolvedItem.item();
        Student student = resolvedItem.student();
        entryContext.findActiveStudent(student.getId());
        entryContext.validateEnrollment(student.getId(), semester, classSubject.getClassId());
        validator.validateScoreValue(item.scoreStatus(), item.scoreValue());

        Optional<StudentScore> existing = scoreWriter.findExisting(columnId, student.getId());

        ResStudentScoreDTO result;
        if (existing.isPresent()) {
            result = scoreWriter.updateExisting(existing.get(), student, item, semester, actorId);
        } else {
            validator.validateCreateVersion(item.expectedVersion());
            result = scoreWriter.createNew(
                    columnId, student, item.scoreStatus(),
                    item.scoreValue(), item.note(), actorId);
        }

        affectedStudentIds.add(student.getId());
        return result;
    }

    @Transactional
    public ResStudentScoreDTO upsertSingleScoreByCode(
            Long columnId, String studentCode, ReqUpsertStudentScoreDTO request) {
        Student student = studentLookupService.resolveStudent(null, studentCode);
        return upsertSingleScore(columnId, student.getId(), request);
    }

    private List<ResolvedBulkScoreItem> resolveBulkItems(List<ReqBulkScoreItemDTO> items) {
        return items.stream()
                .map(item -> new ResolvedBulkScoreItem(
                        item,
                        studentLookupService.resolveStudent(item.studentId(), item.studentCode())))
                .toList();
    }

    private void validateNoDuplicateResolvedStudents(List<ResolvedBulkScoreItem> resolvedItems) {
        Set<Long> seen = new HashSet<>();
        for (ResolvedBulkScoreItem resolvedItem : resolvedItems) {
            Long studentId = resolvedItem.student().getId();
            if (!seen.add(studentId)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Trùng lặp học sinh trong danh sách: " + studentId);
            }
        }
    }

    private record ResolvedBulkScoreItem(ReqBulkScoreItemDTO item, Student student) {
    }
}
