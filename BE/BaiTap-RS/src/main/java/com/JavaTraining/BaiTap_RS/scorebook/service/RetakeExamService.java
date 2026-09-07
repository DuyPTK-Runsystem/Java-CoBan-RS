package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpdateRetakeScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExam;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExamStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.repository.RetakeExamRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class RetakeExamService {

    private final RetakeExamRepository retakeExamRepository;
    private final StudentAnnualTranscriptRepository annualTranscriptRepository;
    private final StudentSubjectAnnualResultRepository annualResultRepository;
    private final TranscriptStateService transcriptStateService;
    private final CalculationTaskService calculationTaskService;
    private final ScorebookAuditService auditService;

    public RetakeExamService(
            RetakeExamRepository retakeExamRepository,
            StudentAnnualTranscriptRepository annualTranscriptRepository,
            StudentSubjectAnnualResultRepository annualResultRepository,
            TranscriptStateService transcriptStateService,
            CalculationTaskService calculationTaskService,
            ScorebookAuditService auditService) {
        this.retakeExamRepository = retakeExamRepository;
        this.annualTranscriptRepository = annualTranscriptRepository;
        this.annualResultRepository = annualResultRepository;
        this.transcriptStateService = transcriptStateService;
        this.calculationTaskService = calculationTaskService;
        this.auditService = auditService;
    }

    @Transactional
    public ResRetakeExamDTO createRetakeExam(ReqCreateRetakeExamDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                RetakeExamService.class,
                "RetakeExamService.createRetakeExam");
        StudentAnnualTranscript annualTranscript = annualTranscriptRepository
                .findByStudentIdAndAcademicYearId(request.studentId(), request.academicYearId())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy bảng điểm tổng kết năm học của học sinh"));

        StudentSubjectAnnualResult subjectResult = annualResultRepository
                .findByAnnualTranscriptIdAndSubjectId(annualTranscript.getId(), request.subjectId())
                .orElseThrow(() -> new AppException(
                        HttpStatus.CONFLICT,
                        "Chưa có kết quả năm học cho môn học này để tạo kỳ thi lại"));

        if (subjectResult.getRegularDtbmhCn() == null) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Chưa có điểm tổng kết thường (regular_dtbmh_cn) cho môn học để tạo kỳ thi lại");
        }

        if (retakeExamRepository.findByStudentIdAndAcademicYearIdAndSubjectId(
                request.studentId(), request.academicYearId(), request.subjectId()).isPresent()) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Đã tồn tại bản ghi thi lại cho học sinh, năm học và môn học này");
        }

        RetakeExamStatus initialStatus = request.retakeScore() != null
                ? RetakeExamStatus.SCORED
                : RetakeExamStatus.PLANNED;

        RetakeExam retakeExam = new RetakeExam(
                request.studentId(),
                request.academicYearId(),
                request.subjectId(),
                subjectResult.getRegularDtbmhCn(),
                initialStatus);
        retakeExam.setRetakeScore(request.retakeScore());
        retakeExam.setExamDate(request.examDate());
        retakeExam.setNote(request.note());
        retakeExam.setCreatedBy(AuditContext.currentUserId());
        retakeExam.setUpdatedBy(AuditContext.currentUserId());

        retakeExam = retakeExamRepository.save(retakeExam);

        auditService.writeAudit(
                "RETAKE_EXAM_CREATED",
                "retake_exam",
                retakeExam.getId(),
                null,
                toAuditSnapshot(retakeExam));

        if (retakeExam.getStatus() == RetakeExamStatus.SCORED) {
            long newVersion = transcriptStateService.touchAnnualTranscript(
                    retakeExam.getStudentId(), retakeExam.getAcademicYearId());
            calculationTaskService.ensureRecalcTask(
                    retakeExam.getStudentId(), retakeExam.getAcademicYearId(), newVersion);
        }

        return toResponse(retakeExam);
    }

    @Transactional
    public ResRetakeExamDTO updateRetakeScore(Long retakeId, ReqUpdateRetakeScoreDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                RetakeExamService.class,
                "RetakeExamService.updateRetakeScore");
        RetakeExam retakeExam = findRetakeExam(retakeId);
        if (retakeExam.getStatus() == RetakeExamStatus.CANCELLED) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "Không thể cập nhật điểm cho bản ghi thi lại đã bị hủy");
        }

        Map<String, Object> beforeData = toAuditSnapshot(retakeExam);

        retakeExam.setRetakeScore(request.retakeScore());
        if (request.examDate() != null) {
            retakeExam.setExamDate(request.examDate());
        }
        if (request.note() != null) {
            retakeExam.setNote(request.note());
        }
        retakeExam.setStatus(RetakeExamStatus.SCORED);
        retakeExam.setUpdatedBy(AuditContext.currentUserId());

        retakeExam = retakeExamRepository.save(retakeExam);

        auditService.writeAudit(
                "RETAKE_EXAM_SCORE_UPDATED",
                "retake_exam",
                retakeExam.getId(),
                beforeData,
                toAuditSnapshot(retakeExam));

        long newVersion = transcriptStateService.touchAnnualTranscript(
                retakeExam.getStudentId(), retakeExam.getAcademicYearId());
        calculationTaskService.ensureRecalcTask(
                retakeExam.getStudentId(), retakeExam.getAcademicYearId(), newVersion);

        return toResponse(retakeExam);
    }

    @Transactional
    public ResRetakeExamDTO cancelRetakeExam(Long retakeId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                RetakeExamService.class,
                "RetakeExamService.cancelRetakeExam");
        RetakeExam retakeExam = findRetakeExam(retakeId);
        if (retakeExam.getStatus() == RetakeExamStatus.CANCELLED) {
            return toResponse(retakeExam);
        }

        Map<String, Object> beforeData = toAuditSnapshot(retakeExam);
        boolean wasScored = retakeExam.getStatus() == RetakeExamStatus.SCORED;

        retakeExam.setStatus(RetakeExamStatus.CANCELLED);
        retakeExam.setUpdatedBy(AuditContext.currentUserId());
        retakeExam = retakeExamRepository.save(retakeExam);

        auditService.writeAudit(
                "RETAKE_EXAM_CANCELLED",
                "retake_exam",
                retakeExam.getId(),
                beforeData,
                toAuditSnapshot(retakeExam));

        if (wasScored) {
            long newVersion = transcriptStateService.touchAnnualTranscript(
                    retakeExam.getStudentId(), retakeExam.getAcademicYearId());
            calculationTaskService.ensureRecalcTask(
                    retakeExam.getStudentId(), retakeExam.getAcademicYearId(), newVersion);
        }

        return toResponse(retakeExam);
    }

    @Transactional(readOnly = true)
    public ResRetakeExamDTO getRetakeExam(Long retakeId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                RetakeExamService.class,
                "RetakeExamService.getRetakeExam");
        return toResponse(findRetakeExam(retakeId));
    }

    @Transactional(readOnly = true)
    public Page<ResRetakeExamDTO> findRetakeExams(ReqFilterRetakeExamDTO filter) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                RetakeExamService.class,
                "RetakeExamService.findRetakeExams");
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return retakeExamRepository.findAll(RetakeExamSpecifications.from(filter), pageable)
                .map(this::toResponse);
    }

    private RetakeExam findRetakeExam(Long retakeId) {
        return retakeExamRepository.findById(retakeId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy bản ghi thi lại"));
    }

    private ResRetakeExamDTO toResponse(RetakeExam exam) {
        return new ResRetakeExamDTO(
                exam.getId(),
                exam.getStudentId(),
                exam.getAcademicYearId(),
                exam.getSubjectId(),
                exam.getPreRetakeScore(),
                exam.getRetakeScore(),
                exam.getExamDate(),
                exam.getStatus(),
                exam.getNote(),
                exam.getCreatedBy(),
                exam.getUpdatedBy(),
                exam.getCreatedAt(),
                exam.getUpdatedAt());
    }

    private Map<String, Object> toAuditSnapshot(RetakeExam exam) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("retakeId", exam.getId());
        map.put("studentId", exam.getStudentId());
        map.put("academicYearId", exam.getAcademicYearId());
        map.put("subjectId", exam.getSubjectId());
        map.put("preRetakeScore", exam.getPreRetakeScore());
        map.put("retakeScore", exam.getRetakeScore());
        map.put("examDate", exam.getExamDate() != null ? exam.getExamDate().toString() : null);
        map.put("status", exam.getStatus() != null ? exam.getStatus().name() : null);
        map.put("note", exam.getNote());
        map.put("createdBy", exam.getCreatedBy());
        map.put("updatedBy", exam.getUpdatedBy());
        return map;
    }
}
