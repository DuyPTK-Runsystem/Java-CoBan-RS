package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.assignment.service.SubjectTeachingAssignmentAccessService;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScorebookGuardTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SubjectTeachingAssignmentAccessService assignmentAccessService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void academicOfficeBypassesTeacherAssignmentCheck() {
        authenticate(100L, "ACADEMIC_OFFICE");
        ScorebookGuard guard = new ScorebookGuard(teacherRepository, assignmentAccessService);

        verifyOfficeBypass(guard);
    }

    @Test
    void mappedTeacherUsesActiveSubjectAssignment() {
        authenticate(100L, "TEACHER");
        Teacher teacher = new Teacher(
                100L,
                "GV001",
                "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                TeacherStatus.ACTIVE);
        ReflectionTestUtils.setField(teacher, "id", 200L);
        Mockito.when(teacherRepository.findByUserId(100L)).thenReturn(Optional.of(teacher));
        ScorebookGuard guard = new ScorebookGuard(teacherRepository, assignmentAccessService);

        verifyTeacherAssignment(guard);
    }

    @Test
    void mappedTeacherCanReadAssignedClassSubject() {
        authenticate(100L, "TEACHER");
        Teacher teacher = new Teacher(
                100L,
                "GV001",
                "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                TeacherStatus.ACTIVE);
        ReflectionTestUtils.setField(teacher, "id", 200L);
        Mockito.when(teacherRepository.findByUserId(100L)).thenReturn(Optional.of(teacher));
        ScorebookGuard guard = new ScorebookGuard(teacherRepository, assignmentAccessService);

        verifyClassSubjectAssignment(guard);
    }

    @Test
    void teacherWithoutMappedPrincipalIsForbidden() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("teacher", null, List.of()));
        verifyMissingPrincipal();
    }

    private void verifyOfficeBypass(ScorebookGuard guard) {
        guard.assertCanManage(new Scorebook(20L, ScorebookStatus.OPEN));
        Mockito.verifyNoInteractions(teacherRepository, assignmentAccessService);
    }

    private void verifyTeacherAssignment(ScorebookGuard guard) {
        guard.assertCanManage(new Scorebook(20L, ScorebookStatus.OPEN));
        Mockito.verify(assignmentAccessService).assertActiveAssignment(
                Mockito.eq(200L), Mockito.eq(20L), Mockito.any());
    }

    private void verifyClassSubjectAssignment(ScorebookGuard guard) {
        guard.assertCanReadClassSubject(20L);
        Mockito.verify(assignmentAccessService).assertActiveAssignment(
                Mockito.eq(200L), Mockito.eq(20L), Mockito.any());
    }

    private void verifyMissingPrincipal() {
        ScorebookGuard guard = new ScorebookGuard(teacherRepository, assignmentAccessService);
        AppException exception = Assertions.assertThrows(
                AppException.class,
                () -> guard.assertCanManage(new Scorebook(20L, ScorebookStatus.OPEN)));
        Assertions.assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatus(),
                "authenticated teacher without a mapped principal must be forbidden");
    }

    private void authenticate(Long userId, String roleCode) {
        User user = new User("user" + userId, "password");
        ReflectionTestUtils.setField(user, "id", userId);
        user.addRole(new Role(roleCode, roleCode, roleCode));
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
