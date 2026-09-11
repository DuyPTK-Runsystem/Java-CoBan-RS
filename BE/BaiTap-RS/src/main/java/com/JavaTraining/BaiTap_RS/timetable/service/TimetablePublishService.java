package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqPublishTimetableDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableDetailDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableReviewDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableAudit;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePublishIntent;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePublishIntentRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetablePublishService {

    private final TimetableHeadRepository headRepository;
    private final TimetableRevisionRepository revisionRepository;
    private final TimetablePublishIntentRepository publishIntentRepository;
    private final TimetableAuditRepository auditRepository;
    private final TimetableValidationService validationService;
    private final TimetableService timetableService;

    @Transactional
    public ResTimetableDetailDTO publish(Long revisionId, ReqPublishTimetableDTO req, String idempotencyKey) {
        Optional<ResTimetableDetailDTO> idempotentResult = checkIdempotency(revisionId, idempotencyKey);
        if (idempotentResult.isPresent()) {
            return idempotentResult.get();
        }

        TimetableRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy bản thời khóa biểu"));
        TimetableHead head = headRepository.findById(revision.getTimetableId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đầu thời khóa biểu"));

        validateVersions(revision, head, req);

        // 2. Re-validate atomically
        ResTimetableReviewDTO review = validationService.validateRevision(revisionId);
        if (review.blockingCount() > 0) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể công bố: còn " + review.blockingCount() + " lỗi chặn chưa được xử lý.");
        }

        // 3. Close previous published revision if exists
        closePreviousRevision(head, revision);

        // 4. Update current revision to PUBLISHED
        revision.setStatus(TimetableRevisionStatus.PUBLISHED);
        revision = revisionRepository.save(revision);

        // 5. Update head & records
        head.setCurrentRevisionId(revision.getId());
        headRepository.save(head);
        recordPublish(head, revision, idempotencyKey);

        return timetableService.getDetail(revision.getId());
    }

    private Optional<ResTimetableDetailDTO> checkIdempotency(Long revisionId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        Optional<TimetablePublishIntent> existingIntent = publishIntentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingIntent.isPresent() && !Objects.equals(existingIntent.get().getRevisionId(), revisionId)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Idempotency-Key đã được sử dụng cho một bản thời khóa biểu khác.");
        }
        return existingIntent.map(intent -> timetableService.getDetail(revisionId));
    }

    private void validateVersions(
            TimetableRevision revision,
            TimetableHead head,
            ReqPublishTimetableDTO req) {
        if (!Objects.equals(revision.getVersion(), req.expectedVersion())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Bản thời khóa biểu đã bị sửa đổi bởi người khác. Vui lòng tải lại.");
        }
        if (req.expectedHeadVersion() != null && !Objects.equals(head.getVersion(), req.expectedHeadVersion())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Đầu thời khóa biểu đã bị sửa đổi bởi người khác. Vui lòng tải lại.");
        }
    }

    private void recordPublish(TimetableHead head, TimetableRevision revision, String idempotencyKey) {
        auditRepository.save(new TimetableAudit(
                head.getId(), revision.getId(), "PUBLISH", AuditContext.currentUserId(),
                "Công bố thành công revision " + revision.getRevisionNumber()));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            publishIntentRepository.save(new TimetablePublishIntent(
                    revision.getId(), idempotencyKey, AuditContext.currentUserId(), null));
        }
    }

    private void closePreviousRevision(TimetableHead head, TimetableRevision revision) {
        if (head.getCurrentRevisionId() == null || Objects.equals(head.getCurrentRevisionId(), revision.getId())) {
            return;
        }
        final LocalDate effectiveFrom = revision.getEffectiveFrom();
        revisionRepository.findById(head.getCurrentRevisionId()).ifPresent(prevRev -> {
            if (prevRev.getStatus() == TimetableRevisionStatus.PUBLISHED) {
                prevRev.setStatus(TimetableRevisionStatus.ARCHIVED);
                if (effectiveFrom != null) {
                    prevRev.setEffectiveTo(effectiveFrom.minusDays(1));
                }
                revisionRepository.save(prevRev);
            }
        });
    }
}
