package com.JavaTraining.BaiTap_RS.notification;

import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationAudienceService;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
                "PMD.AvoidDuplicateLiterals",
                "PMD.UnitTestAssertionsShouldIncludeMessage",
                "PMD.UnitTestContainsTooManyAsserts"
})
class NotificationAudienceServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private SchoolClassRepository schoolClassRepository;

        @Mock
        private StudentYearEnrollmentRepository studentYearEnrollmentRepository;

        @Mock
        private StudentRepository studentRepository;

        private NotificationAudienceService audienceService;

        @BeforeEach
        void setUp() {
                audienceService = new NotificationAudienceService(
                                userRepository,
                                schoolClassRepository,
                                studentYearEnrollmentRepository,
                                studentRepository);
        }

        @Test
        void resolveIndividualAudienceSuccess() {
                Notification notification = new Notification(
                                "Tiêu đề",
                                "Nội dung",
                                NotificationAudienceType.INDIVIDUAL,
                                "10, 20",
                                1L,
                                "DEFAULT_SCHOOL");

                User u1 = new User("user10", "pass");
                ReflectionTestUtils.setField(u1, "id", 10L);
                User u2 = new User("user20", "pass");
                ReflectionTestUtils.setField(u2, "id", 20L);

                Mockito.when(userRepository.findAllById(ArgumentMatchers.any())).thenReturn(List.of(u1, u2));

                Set<Long> result = audienceService.resolveAudienceUserIds(notification, null);

                Assertions.assertEquals(2, result.size());
                Assertions.assertTrue(result.contains(10L));
                Assertions.assertTrue(result.contains(20L));
        }

        @Test
        void resolveIndividualAudienceUserNotFoundThrowsUnprocessable() {
                Notification notification = new Notification(
                                "Tiêu đề",
                                "Nội dung",
                                NotificationAudienceType.INDIVIDUAL,
                                "999",
                                1L,
                                "DEFAULT_SCHOOL");

                Mockito.when(userRepository.findAllById(ArgumentMatchers.any())).thenReturn(List.of());

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> audienceService.resolveAudienceUserIds(notification, null));
                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void resolveClassAudienceSuccess() {
                Notification notification = new Notification(
                                "Tiêu đề",
                                "Nội dung",
                                NotificationAudienceType.CLASS,
                                "50",
                                1L,
                                "DEFAULT_SCHOOL");

                Mockito.when(schoolClassRepository.existsById(50L)).thenReturn(true);

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                StudentYearEnrollment e1 = new StudentYearEnrollment(1L, 1L, 50L, EnrollmentStatus.ACTIVE, now);
                StudentYearEnrollment e2 = new StudentYearEnrollment(2L, 1L, 50L, EnrollmentStatus.ACTIVE, now);

                Mockito.when(studentYearEnrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                                50L, EnrollmentStatus.ACTIVE))
                                .thenReturn(List.of(e1, e2));

                Student s1 = new Student("Nguyen Van A", "HS001");
                ReflectionTestUtils.setField(s1, "id", 1L);
                ReflectionTestUtils.setField(s1, "userId", 101L);

                Student s2 = new Student("Nguyen Van B", "HS002");
                ReflectionTestUtils.setField(s2, "id", 2L);
                ReflectionTestUtils.setField(s2, "userId", null);

                Mockito.when(studentRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(s1, s2));

                Set<Long> result = audienceService.resolveAudienceUserIds(notification, null);

                Assertions.assertEquals(1, result.size());
                Assertions.assertTrue(result.contains(101L));
        }

        @Test
        void resolveClassAudienceClassNotFoundThrowsNotFound() {
                Notification notification = new Notification(
                                "Tiêu đề",
                                "Nội dung",
                                NotificationAudienceType.CLASS,
                                "9999",
                                1L,
                                "DEFAULT_SCHOOL");

                Mockito.when(schoolClassRepository.existsById(9999L)).thenReturn(false);

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> audienceService.resolveAudienceUserIds(notification, null));
                Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }

        @Test
        void resolveSchoolAudienceSuccess() {
                Notification notification = new Notification(
                                "Tiêu đề",
                                "Nội dung",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");

                User u1 = new User("user1", "pass");
                ReflectionTestUtils.setField(u1, "id", 1L);
                User u2 = new User("user2", "pass");
                ReflectionTestUtils.setField(u2, "id", 2L);

                Mockito.when(userRepository.findAll()).thenReturn(List.of(u1, u2));

                Set<Long> result = audienceService.resolveAudienceUserIds(notification, null);

                Assertions.assertEquals(2, result.size());
                Assertions.assertTrue(result.contains(1L));
                Assertions.assertTrue(result.contains(2L));
        }
}
