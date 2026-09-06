package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferTargetScoreDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferWithScoresDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResTransferScoreAssistDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResTransferWithScoresDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResEnrollmentMutationDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.CalculationTaskService;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScoreEntryWriter;
import com.JavaTraining.BaiTap_RS.scorebook.service.TranscriptStateService;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentTransferScoreService {

    private final EnrollmentService enrollmentService;
    private final EnrollmentTransferScoreContextService contextService;
    private final EnrollmentTransferScoreAssistService assistService;
    private final EnrollmentTransferScoreValidationService validationService;
    private final ScoreEntryWriter scoreWriter;
    private final TranscriptStateService transcriptService;
    private final CalculationTaskService taskService;

    public EnrollmentTransferScoreService(
            EnrollmentService enrollmentService,
            EnrollmentTransferScoreContextService contextService,
            EnrollmentTransferScoreAssistService assistService,
            EnrollmentTransferScoreValidationService validationService,
            ScoreEntryWriter scoreWriter,
            TranscriptStateService transcriptService,
            CalculationTaskService taskService) {
        this.enrollmentService = enrollmentService;
        this.contextService = contextService;
        this.assistService = assistService;
        this.validationService = validationService;
        this.scoreWriter = scoreWriter;
        this.transcriptService = transcriptService;
        this.taskService = taskService;
    }

    @Transactional(readOnly = true)
    public ResTransferScoreAssistDTO getAssist(Long enrollmentId, Long targetClassId, Long semesterId) {
        TransferScoreContext context = contextService.loadContext(enrollmentId, targetClassId, semesterId);
        TransferScoreData data = contextService.loadScoreData(new TransferScoreDataRequest(
                context.sourceClass(), context.targetClass(), context.semester(), context.student()));
        return assistService.toAssist(context, data);
    }

    @Transactional
    public ResTransferWithScoresDTO transferWithScores(Long enrollmentId, ReqTransferWithScoresDTO request) {
        TransferScoreContext context = contextService.loadContext(enrollmentId, request.targetClassId(), null);
        Semester semester = contextService.resolveSemesterForMutation(enrollmentId, request);
        TransferScoreData data = contextService.loadScoreData(new TransferScoreDataRequest(
                context.sourceClass(), context.targetClass(), semester, context.student()));
        List<PreparedTransferScore> prepared = validationService.validateScores(
                new TransferScoreValidationRequest(request.scores(), data, context.student(), semester));

        return transferAndWrite(enrollmentId, request, context, semester, prepared);
    }

    private ResTransferWithScoresDTO transferAndWrite(
            Long enrollmentId,
            ReqTransferWithScoresDTO request,
            TransferScoreContext context,
            Semester semester,
            List<PreparedTransferScore> prepared) {
        ResEnrollmentMutationDTO transfer = enrollmentService.transferEnrollment(enrollmentId,
                new ReqTransferEnrollmentDTO(request.targetClassId(), request.effectiveAt(), request.reason()));
        Long actorId = AuditContext.currentUserId();
        List<ResStudentScoreDTO> savedScores = prepared.stream()
                .map(item -> writeScore(item, context.student(), semester, actorId)).toList();
        if (!savedScores.isEmpty()) {
            long sourceVersion = transcriptService.touchTranscripts(
                    context.student().getId(), semester.getAcademicYearId(), semester.getId());
            taskService.ensureRecalcTask(context.student().getId(), semester.getAcademicYearId(), sourceVersion);
        }
        return new ResTransferWithScoresDTO(transfer, savedScores);
    }

    private ResStudentScoreDTO writeScore(
            PreparedTransferScore item, Student student, Semester semester, Long actorId) {
        ReqTransferTargetScoreDTO request = item.request();
        if (item.existing() == null) {
            return scoreWriter.createNew(
                    item.column().getId(), student, request.scoreStatus(), request.scoreValue(), request.note(), actorId);
        }
        return scoreWriter.updateExisting(item.existing(), student,
                new ReqUpsertStudentScoreDTO(
                        request.scoreStatus(), request.scoreValue(), request.note(), request.expectedVersion()),
                semester, actorId);
    }
}
