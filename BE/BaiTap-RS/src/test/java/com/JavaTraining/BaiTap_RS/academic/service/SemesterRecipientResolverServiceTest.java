package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ClassSubjectIncompleteDetail;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterRecipientInfo;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class SemesterRecipientResolverServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private TeacherRepository teacherRepository;

        @Mock
        private SemesterRepository semesterRepository;

        @Mock
        private SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;

        @Mock
        private HomeroomAssignmentRepository homeroomAssignmentRepository;

        private SemesterRecipientResolverService resolverService;

        @BeforeEach
        void setUp() {
                SemesterNotificationTemplateService templateService = new SemesterNotificationTemplateService();
                resolverService = new SemesterRecipientResolverService(
                                userRepository,
                                teacherRepository,
                                semesterRepository,
                                subjectTeachingAssignmentRepository,
                                homeroomAssignmentRepository,
                                templateService);
        }

        @Test
        void resolvesAcademicOfficeAndTeachers() {
                Semester semester = new Semester(
                                1L, "HK1", "Học kỳ 1", 1, LocalDate.of(2026, 9, 1),
                                LocalDate.of(2027, 1, 15), null,
                                com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus.ACTIVE);
                Mockito.when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));

                User adminUser = new User("admin@school.edu.vn", "password");
                Mockito.when(userRepository.findAcademicOfficeAndAdminUsers()).thenReturn(List.of(adminUser));

                Teacher mathTeacher = new Teacher(
                                2L, "GV01", "Thầy Toán", LocalDate.of(1985, 5, 20), "MALE", "0900000001",
                                "math@school.edu.vn", "Toán", LocalDate.of(2015, 9, 1), TeacherStatus.ACTIVE);
                Teacher homeroomTeacher = new Teacher(
                                3L, "GV02", "Cô Chủ Nhiệm", LocalDate.of(1988, 3, 15), "FEMALE", "0900000002",
                                "homeroom@school.edu.vn", "Văn", LocalDate.of(2018, 9, 1), TeacherStatus.ACTIVE);

                SubjectTeachingAssignment subjAssignment = new SubjectTeachingAssignment(
                                100L, 100L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);
                HomeroomAssignment hrAssignment = new HomeroomAssignment(
                                10L, 200L, LocalDate.of(2026, 9, 1), null, AssignmentStatus.ACTIVE, 1L);

                Mockito.when(subjectTeachingAssignmentRepository.findFirstByClassSubjectIdAndStatus(100L,
                                AssignmentStatus.ACTIVE))
                                .thenReturn(Optional.of(subjAssignment));
                Mockito.when(homeroomAssignmentRepository.findFirstByClassIdAndStatus(10L, AssignmentStatus.ACTIVE))
                                .thenReturn(Optional.of(hrAssignment));

                Mockito.when(teacherRepository.findById(100L)).thenReturn(Optional.of(mathTeacher));
                Mockito.when(teacherRepository.findById(200L)).thenReturn(Optional.of(homeroomTeacher));

                SemesterCompletenessSummaryDTO summary = new SemesterCompletenessSummaryDTO(
                                false, 0, 0, 0, 2, 0, 0, 0, List.of("Thiếu điểm môn Toán lớp 10A1"));

                ClassSubjectIncompleteDetail detail = new ClassSubjectIncompleteDetail(
                                100L, 10L, 5L, "10A1", "Toán", List.of("Học sinh 1 chưa nhập điểm miệng"));

                List<SemesterRecipientInfo> recipients = resolverService.resolveRecipients(
                                1L, "t-7d", summary, List.of(detail));

                Assertions.assertEquals(3, recipients.size(), "recipients size should be 3");

                SemesterRecipientInfo adminRecip = recipients.stream()
                                .filter(r -> "ACADEMIC_OFFICE".equals(r.recipientRole()))
                                .findFirst().orElseThrow();
                Assertions.assertEquals("admin@school.edu.vn", adminRecip.recipientEmail(),
                                "admin recipient email should match");

                SemesterRecipientInfo subjRecip = recipients.stream()
                                .filter(r -> "SUBJECT_TEACHER".equals(r.recipientRole()))
                                .findFirst().orElseThrow();
                Assertions.assertEquals("math@school.edu.vn", subjRecip.recipientEmail(),
                                "math teacher email should match");
                Assertions.assertTrue(subjRecip.bodyContent().contains("Học sinh 1 chưa nhập điểm miệng"),
                                "math teacher body should contain issue detail");

                SemesterRecipientInfo hrRecip = recipients.stream()
                                .filter(r -> "HOMEROOM_TEACHER".equals(r.recipientRole()))
                                .findFirst().orElseThrow();
                Assertions.assertEquals("homeroom@school.edu.vn", hrRecip.recipientEmail(),
                                "homeroom teacher email should match");
        }
}
