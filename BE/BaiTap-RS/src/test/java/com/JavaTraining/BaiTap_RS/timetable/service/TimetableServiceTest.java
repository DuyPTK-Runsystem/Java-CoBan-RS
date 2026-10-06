package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateRevisionDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateTimetableDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqConfirmTimetableTeacherLoadPolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqUpdateTimetableEntriesDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableDetailDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableSummaryDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicyStatus;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadPolicyRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentWorkspaceCapabilityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class TimetableServiceTest {

        @Mock
        private TimetableHeadRepository headRepository;

        @Mock
        private TimetableRevisionRepository revisionRepository;

        @Mock
        private TimetableEntryRepository entryRepository;

        @Mock
        private TimetablePeriodRepository periodRepository;

        @Mock
        private TimetableAuditRepository auditRepository;

        @Mock
        private SemesterRepository semesterRepository;

        @Mock
        private ClassSubjectRepository classSubjectRepository;

        @Mock
        private SchoolClassRepository schoolClassRepository;

        @Mock
        private SubjectRepository subjectRepository;

        @Mock
        private TeacherRepository teacherRepository;

        @Mock
        private FunctionalRoomRepository roomRepository;

        @Mock
        private SubjectTeachingAssignmentRepository assignmentRepository;

        @Mock
        private TeacherLoadPolicyRepository policyRepository;

        @Mock
        private TimetableValidationService validationService;

        @Mock
        private TimetableCalendarService calendarService;

        @Mock
        private TimetableAgentWorkspaceCapabilityService timetableAgentCapabilityService;

        private TimetableService timetableService;

        @BeforeEach
        void setUp() {
                timetableService = new TimetableService(
                                headRepository, revisionRepository, entryRepository, periodRepository,
                                auditRepository, semesterRepository, policyRepository, assignmentRepository,
                                classSubjectRepository, schoolClassRepository, subjectRepository,
                                teacherRepository, roomRepository, validationService, calendarService,
                                timetableAgentCapabilityService);
        }

        @Test
        void createDraft_semesterNotFound_throwsNotFound() {
                Mockito.when(semesterRepository.findById(99L)).thenReturn(Optional.empty());

                ReqCreateTimetableDTO req = new ReqCreateTimetableDTO(99L, LocalDate.of(2026, 9, 1), null, null, null);
                AppException ex = Assertions.assertThrows(AppException.class, () -> timetableService.createDraft(req));
                Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }

        @Test
        void createDraft_success() {
                Semester semester = new Semester(
                                1L, "HK1", "Học kỳ 1", 1, LocalDate.of(2026, 9, 1),
                                LocalDate.of(2027, 1, 15), null, SemesterStatus.ACTIVE);
                Mockito.when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));

                TimetableHead head = new TimetableHead(1L);
                ReflectionTestUtils.setField(head, "id", 10L);
                Mockito.when(headRepository.save(Mockito.any())).thenReturn(head);

                TimetableRevision rev = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(rev, "id", 100L);
                Mockito.when(revisionRepository.save(Mockito.any())).thenReturn(rev);

                ReqCreateTimetableDTO req = new ReqCreateTimetableDTO(1L, LocalDate.of(2026, 9, 1), null, null, null);
                ResTimetableDetailDTO detail = timetableService.createDraft(req);

                Assertions.assertNotNull(detail);
                Assertions.assertEquals(100L, detail.revisionId());
                Assertions.assertEquals(TimetableRevisionStatus.DRAFT, detail.status());
                Assertions.assertEquals(1, detail.revisionNumber());
        }

        @Test
        void confirmTeacherLoadPolicy_bindsCurrentPolicyAuditsAndBumpsVersion() {
                TimetableRevision revision = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(revision, "id", 100L);
                Mockito.when(revisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(revision));
                TeacherLoadPolicy activePolicy = new TeacherLoadPolicy(
                                "2026-27", "Approved source", LocalDate.of(2026, 9, 1), null, 19, 4, 3);
                ReflectionTestUtils.setField(activePolicy, "id", 20L);
                activePolicy.setStatus(TeacherLoadPolicyStatus.ACTIVE);
                Mockito.when(policyRepository.findById(20L)).thenReturn(Optional.of(activePolicy));
                Mockito.when(policyRepository.findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE))
                                .thenReturn(Optional.of(activePolicy));
                revision.setValidationFingerprint("old-fingerprint");
                revision.setBlockingCount(2);
                revision.setWarningCount(1);
                Mockito.when(revisionRepository.saveAndFlush(revision)).thenReturn(revision);
                Mockito.when(headRepository.findById(10L)).thenReturn(Optional.empty());
                Mockito.when(semesterRepository.findById(1L)).thenReturn(Optional.empty());

                ResTimetableDetailDTO result = timetableService.confirmTeacherLoadPolicy(
                                100L, new ReqConfirmTimetableTeacherLoadPolicyDTO(20L, 0L));

                Assertions.assertEquals(20L, revision.getPolicyId());
                Assertions.assertEquals(0L, result.version());
                Assertions.assertEquals("2026-27", result.policyVersion());
                Assertions.assertNull(revision.getValidationFingerprint());
                Assertions.assertEquals(0, revision.getBlockingCount());
                Assertions.assertEquals(0, revision.getWarningCount());
                Mockito.verify(auditRepository).save(Mockito.argThat(audit ->
                                "CONFIRM_TEACHER_LOAD_POLICY".equals(audit.getAction())
                                                && audit.getRevisionId().equals(100L)));
                Mockito.verify(revisionRepository).saveAndFlush(revision);
        }

        @Test
        void confirmTeacherLoadPolicy_staleVersionThrowsConflict() {
                TimetableRevision revision = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(revision, "id", 100L);
                revision.setVersion(2L);
                Mockito.when(revisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(revision));

                AppException ex = Assertions.assertThrows(AppException.class, () -> timetableService
                                .confirmTeacherLoadPolicy(100L,
                                                new ReqConfirmTimetableTeacherLoadPolicyDTO(20L, 1L)));

                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
                Mockito.verifyNoInteractions(policyRepository, auditRepository);
        }

        @Test
        void confirmTeacherLoadPolicy_rejectsNonDraft() {
                TimetableRevision revision = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(revision, "id", 100L);
                revision.setStatus(TimetableRevisionStatus.VALIDATED);
                Mockito.when(revisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(revision));

                AppException ex = Assertions.assertThrows(AppException.class, () -> timetableService
                                .confirmTeacherLoadPolicy(100L,
                                                new ReqConfirmTimetableTeacherLoadPolicyDTO(20L, 0L)));

                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
                Mockito.verifyNoInteractions(policyRepository, auditRepository);
        }

        @Test
        void confirmTeacherLoadPolicy_rejectsWhenThereIsNoActivePolicy() {
                TimetableRevision revision = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(revision, "id", 100L);
                Mockito.when(revisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(revision));
                TeacherLoadPolicy selectedPolicy = new TeacherLoadPolicy(
                                "draft", "Source", LocalDate.of(2026, 9, 1), null, 19, 4, 3);
                ReflectionTestUtils.setField(selectedPolicy, "id", 20L);
                Mockito.when(policyRepository.findById(20L)).thenReturn(Optional.of(selectedPolicy));
                Mockito.when(policyRepository.findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE))
                                .thenReturn(Optional.empty());

                AppException ex = Assertions.assertThrows(AppException.class, () -> timetableService
                                .confirmTeacherLoadPolicy(100L,
                                                new ReqConfirmTimetableTeacherLoadPolicyDTO(20L, 0L)));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
                Mockito.verifyNoInteractions(auditRepository);
        }

        @Test
        void confirmTeacherLoadPolicy_rejectsSelectedPolicyThatIsNotCurrentActive() {
                TimetableRevision revision = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(revision, "id", 100L);
                Mockito.when(revisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(revision));
                TeacherLoadPolicy selectedPolicy = new TeacherLoadPolicy(
                                "old", "Source", LocalDate.of(2025, 9, 1), null, 19, 4, 3);
                ReflectionTestUtils.setField(selectedPolicy, "id", 20L);
                TeacherLoadPolicy currentPolicy = new TeacherLoadPolicy(
                                "current", "Source", LocalDate.of(2026, 9, 1), null, 19, 4, 3);
                ReflectionTestUtils.setField(currentPolicy, "id", 21L);
                currentPolicy.setStatus(TeacherLoadPolicyStatus.ACTIVE);
                Mockito.when(policyRepository.findById(20L)).thenReturn(Optional.of(selectedPolicy));
                Mockito.when(policyRepository.findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE))
                                .thenReturn(Optional.of(currentPolicy));

                AppException ex = Assertions.assertThrows(AppException.class, () -> timetableService
                                .confirmTeacherLoadPolicy(100L,
                                                new ReqConfirmTimetableTeacherLoadPolicyDTO(20L, 0L)));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
                Mockito.verifyNoInteractions(auditRepository);
                Mockito.verify(revisionRepository, Mockito.never()).saveAndFlush(Mockito.any());
        }

        @Test
        void confirmTeacherLoadPolicy_reconfirmingSameCurrentPolicyIsNoOp() {
                TimetableRevision revision = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, 20L);
                ReflectionTestUtils.setField(revision, "id", 100L);
                Mockito.when(revisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(revision));
                TeacherLoadPolicy activePolicy = new TeacherLoadPolicy(
                                "2026-27", "Approved source", LocalDate.of(2026, 9, 1), null, 19, 4, 3);
                ReflectionTestUtils.setField(activePolicy, "id", 20L);
                activePolicy.setStatus(TeacherLoadPolicyStatus.ACTIVE);
                Mockito.when(policyRepository.findById(20L)).thenReturn(Optional.of(activePolicy));
                Mockito.when(policyRepository.findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE))
                                .thenReturn(Optional.of(activePolicy));
                Mockito.when(headRepository.findById(10L)).thenReturn(Optional.empty());
                Mockito.when(semesterRepository.findById(1L)).thenReturn(Optional.empty());

                ResTimetableDetailDTO result = timetableService.confirmTeacherLoadPolicy(
                                100L, new ReqConfirmTimetableTeacherLoadPolicyDTO(20L, 0L));

                Assertions.assertEquals(0L, result.version());
                Mockito.verify(revisionRepository, Mockito.never()).saveAndFlush(Mockito.any());
                Mockito.verifyNoInteractions(auditRepository);
        }

        @Test
        void pageSummaries_exposesEachRevisionIdWhenHeadIsShared() {
                TimetableRevision firstRevision = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(firstRevision, "id", 101L);
                TimetableRevision secondRevision = new TimetableRevision(10L, 1L, 2,
                                LocalDate.of(2026, 10, 1), null, null);
                ReflectionTestUtils.setField(secondRevision, "id", 102L);
                Mockito.when(revisionRepository.findBySemesterId(Mockito.eq(1L), Mockito.any()))
                                .thenReturn(new PageImpl<>(List.of(firstRevision, secondRevision),
                                                PageRequest.of(0, 10), 2));

                Semester semester = new Semester(2026L, "HK1", "HK1 2026 - 2027", 1,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 15), null,
                                SemesterStatus.ACTIVE);
                ReflectionTestUtils.setField(semester, "id", 1L);
                Mockito.when(semesterRepository.findAllById(List.of(1L))).thenReturn(List.of(semester));

                TimetableHead head = new TimetableHead(1L);
                ReflectionTestUtils.setField(head, "id", 10L);
                Mockito.when(headRepository.findAllById(List.of(10L))).thenReturn(List.of(head));

                ResultPaginationDTO<ResTimetableSummaryDTO> result =
                                timetableService.pageSummaries(1L, PageRequest.of(0, 10));

                Assertions.assertEquals(2, result.result().size());
                Assertions.assertEquals(101L, result.result().get(0).revisionId());
                Assertions.assertEquals(102L, result.result().get(1).revisionId());
                Assertions.assertEquals(10L, result.result().get(0).timetableId());
                Assertions.assertEquals(10L, result.result().get(1).timetableId());
        }

        @Test
        void updateEntries_publishedRevision_throwsBadRequest() {
                TimetableRevision rev = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(rev, "id", 100L);
                rev.setStatus(TimetableRevisionStatus.PUBLISHED);
                Mockito.when(revisionRepository.findById(100L)).thenReturn(Optional.of(rev));

                ReqUpdateTimetableEntriesDTO req = new ReqUpdateTimetableEntriesDTO(0L, List.of(), List.of(1L));
                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> timetableService.updateEntries(100L, req));
                Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        void updateEntries_versionConflict_throwsConflict() {
                TimetableRevision rev = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), null, null);
                ReflectionTestUtils.setField(rev, "id", 100L);
                ReflectionTestUtils.setField(rev, "version", 2L);
                rev.setStatus(TimetableRevisionStatus.DRAFT);
                Mockito.when(revisionRepository.findById(100L)).thenReturn(Optional.of(rev));

                ReqUpdateTimetableEntriesDTO req = new ReqUpdateTimetableEntriesDTO(1L, List.of(), List.of());
                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> timetableService.updateEntries(100L, req));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        void createRevision_success_incrementsRevisionNumber() {
                TimetableRevision source = new TimetableRevision(10L, 1L, 1,
                                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), null);
                ReflectionTestUtils.setField(source, "id", 100L);
                Mockito.when(revisionRepository.findById(100L)).thenReturn(Optional.of(source));

                TimetableHead head = new TimetableHead(1L);
                ReflectionTestUtils.setField(head, "id", 10L);
                Mockito.when(headRepository.findById(10L)).thenReturn(Optional.of(head));

                Mockito.when(revisionRepository.findMaxRevisionNumber(10L)).thenReturn(1);

                TimetableRevision rev2 = new TimetableRevision(10L, 1L, 2,
                                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), null);
                ReflectionTestUtils.setField(rev2, "id", 200L);
                Mockito.when(revisionRepository.save(Mockito.any())).thenReturn(rev2);

                Mockito.when(entryRepository.findByRevisionId(100L)).thenReturn(List.of());

                Semester semester = new Semester(
                                1L, "HK1", "Học kỳ 1", 1, LocalDate.of(2026, 9, 1),
                                LocalDate.of(2027, 1, 15), null, SemesterStatus.ACTIVE);
                Mockito.when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));

                ReqCreateRevisionDTO req = new ReqCreateRevisionDTO(0L, LocalDate.of(2026, 10, 1));
                ResTimetableDetailDTO detail = timetableService.createRevision(100L, req);

                Assertions.assertNotNull(detail);
                Assertions.assertEquals(200L, detail.revisionId());
                Assertions.assertEquals(2, detail.revisionNumber());
        }
}
