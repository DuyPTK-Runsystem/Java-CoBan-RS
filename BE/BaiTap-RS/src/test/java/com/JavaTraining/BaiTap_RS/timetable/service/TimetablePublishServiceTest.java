package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqPublishTimetableDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableDetailDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableReviewDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePublishIntent;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePublishIntentRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class TimetablePublishServiceTest {

        @Mock
        private TimetableRevisionRepository revisionRepository;

        @Mock
        private TimetableHeadRepository headRepository;

        @Mock
        private TimetableAuditRepository auditRepository;

        @Mock
        private TimetablePublishIntentRepository publishIntentRepository;

        @Mock
        private TimetableValidationService validationService;

        @Mock
        private TimetableService timetableService;

        private TimetablePublishService publishService;

        @BeforeEach
        void setUp() {
                publishService = new TimetablePublishService(
                                headRepository, revisionRepository, publishIntentRepository,
                                auditRepository, validationService, timetableService);
        }

        @Test
        void publish_idempotentSecondCall_returnsCachedDetail() {
                TimetablePublishIntent intent = new TimetablePublishIntent(10L, "key-123", 1L, null);
                Mockito.when(publishIntentRepository.findByIdempotencyKey("key-123"))
                                .thenReturn(Optional.of(intent));

                ResTimetableDetailDTO mockDetail = new ResTimetableDetailDTO(
                                1L, 1L, "HK1", 10L, 1, TimetableRevisionStatus.PUBLISHED,
                                LocalDate.of(2026, 9, 1), null, null, null,
                                0L, 0L, 0, 0, List.of());
                Mockito.when(timetableService.getDetail(10L)).thenReturn(mockDetail);

                ReqPublishTimetableDTO req = new ReqPublishTimetableDTO(0L, 0L);
                ResTimetableDetailDTO result = publishService.publish(10L, req, "key-123");

                Assertions.assertEquals(10L, result.revisionId());
                Assertions.assertEquals(TimetableRevisionStatus.PUBLISHED, result.status());
        }

        @Test
        void publish_idempotentUsedForDifferentRevision_throwsConflict() {
                TimetablePublishIntent intent = new TimetablePublishIntent(99L, "key-123", 1L, null);
                Mockito.when(publishIntentRepository.findByIdempotencyKey("key-123"))
                                .thenReturn(Optional.of(intent));

                ReqPublishTimetableDTO req = new ReqPublishTimetableDTO(0L, 0L);
                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> publishService.publish(10L, req, "key-123"));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        void publish_blockingIssuesExist_throwsUnprocessableEntity() {
                TimetableRevision rev = new TimetableRevision(1L, 1L, 1,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), null);
                ReflectionTestUtils.setField(rev, "id", 10L);
                ReflectionTestUtils.setField(rev, "version", 0L);
                Mockito.when(revisionRepository.findById(10L)).thenReturn(Optional.of(rev));

                TimetableHead head = new TimetableHead(1L);
                ReflectionTestUtils.setField(head, "id", 1L);
                ReflectionTestUtils.setField(head, "version", 0L);
                Mockito.when(headRepository.findById(1L)).thenReturn(Optional.of(head));

                // Validation returns 2 blocking issues
                ResTimetableReviewDTO review = new ResTimetableReviewDTO(
                                10L, 0L, LocalDateTime.now(), TimetableRevisionStatus.DRAFT, 2, 0, List.of(),
                                List.of());
                Mockito.when(validationService.validateRevision(10L)).thenReturn(review);

                ReqPublishTimetableDTO req = new ReqPublishTimetableDTO(0L, 0L);
                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> publishService.publish(10L, req, null));
                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void publish_success_archivesPreviousRevisionAndPublishes() {
                TimetableRevision rev = new TimetableRevision(1L, 1L, 2,
                                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), null);
                ReflectionTestUtils.setField(rev, "id", 20L);
                ReflectionTestUtils.setField(rev, "version", 0L);
                Mockito.when(revisionRepository.findById(20L)).thenReturn(Optional.of(rev));

                // Head currently points to rev 10
                TimetableHead head = new TimetableHead(1L);
                ReflectionTestUtils.setField(head, "id", 1L);
                ReflectionTestUtils.setField(head, "version", 0L);
                head.setCurrentRevisionId(10L);
                Mockito.when(headRepository.findById(1L)).thenReturn(Optional.of(head));

                // Previous revision 10 is PUBLISHED
                TimetableRevision prevRev = new TimetableRevision(1L, 1L, 1,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), null);
                ReflectionTestUtils.setField(prevRev, "id", 10L);
                prevRev.setStatus(TimetableRevisionStatus.PUBLISHED);
                Mockito.when(revisionRepository.findById(10L)).thenReturn(Optional.of(prevRev));

                // Zero blocking issues
                ResTimetableReviewDTO review = new ResTimetableReviewDTO(
                                20L, 0L, LocalDateTime.now(), TimetableRevisionStatus.DRAFT, 0, 0, List.of(),
                                List.of());
                Mockito.when(validationService.validateRevision(20L)).thenReturn(review);

                Mockito.when(revisionRepository.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

                ReqPublishTimetableDTO req = new ReqPublishTimetableDTO(0L, 0L);
                publishService.publish(20L, req, "key-publish-20");

                // Previous revision should be ARCHIVED and effectiveTo updated to 2026-09-30
                Assertions.assertEquals(TimetableRevisionStatus.ARCHIVED, prevRev.getStatus());
                Assertions.assertEquals(LocalDate.of(2026, 9, 30), prevRev.getEffectiveTo());

                // Current revision should be PUBLISHED
                Assertions.assertEquals(TimetableRevisionStatus.PUBLISHED, rev.getStatus());

                // Head currentRevisionId updated to 20
                Assertions.assertEquals(20L, head.getCurrentRevisionId());

                // Publish intent & audit recorded
                Mockito.verify(auditRepository).save(Mockito.any());
                Mockito.verify(publishIntentRepository).save(Mockito.any());
        }
}
