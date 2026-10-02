package com.JavaTraining.BaiTap_RS.timetable.controller;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqConfirmTimetableTeacherLoadPolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableDetailDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.service.TimetablePublishService;
import com.JavaTraining.BaiTap_RS.timetable.service.TimetableService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.mockito.Mockito;

class TimetableControllerTest {

    @Test
    void confirmTeacherLoadPolicyDelegatesRequestToService() {
        TimetableService service = Mockito.mock(TimetableService.class);
        TimetableController controller = new TimetableController(
                service, Mockito.mock(TimetablePublishService.class), Mockito.mock(TeacherRepository.class));
        ReqConfirmTimetableTeacherLoadPolicyDTO request = new ReqConfirmTimetableTeacherLoadPolicyDTO(5L, 3L);
        ResTimetableDetailDTO expected = new ResTimetableDetailDTO(
                1L, 1L, "HK1", 8L, 1, TimetableRevisionStatus.DRAFT,
                LocalDate.of(2026, 9, 1), null, 5L, "2026-27", 3L, 1L,
                0, 0, List.of("EDIT_ENTRIES"), true);
        Mockito.when(service.confirmTeacherLoadPolicy(8L, request)).thenReturn(expected);

        Assertions.assertSame(expected, controller.confirmTeacherLoadPolicy(8L, request),
                "Controller should return the service's updated timetable detail");
    }

    @Test
    void confirmTeacherLoadPolicyUsesOfficePutRouteAndRole() throws Exception {
        Method method = TimetableController.class.getMethod(
                "confirmTeacherLoadPolicy", Long.class, ReqConfirmTimetableTeacherLoadPolicyDTO.class);
        boolean correctRoute = java.util.Arrays.equals(
                new String[] { "/{id}/teacher-load-policy" }, method.getAnnotation(PutMapping.class).value());
        boolean correctRole = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')"
                .equals(method.getAnnotation(PreAuthorize.class).value());

        Assertions.assertTrue(correctRoute && correctRole,
                "Policy confirmation must use the office-only PUT route");
    }

    @Test
    void confirmTeacherLoadPolicyValidatesBodyAndRequiredFields() throws Exception {
        Method method = TimetableController.class.getMethod(
                "confirmTeacherLoadPolicy", Long.class, ReqConfirmTimetableTeacherLoadPolicyDTO.class);
        boolean validBody = method.getParameters()[1].isAnnotationPresent(Valid.class);
        boolean requiredPolicy = ReqConfirmTimetableTeacherLoadPolicyDTO.class
                .getMethod("policyId").isAnnotationPresent(NotNull.class)
                && ReqConfirmTimetableTeacherLoadPolicyDTO.class.getMethod("policyId")
                        .isAnnotationPresent(Positive.class);
        boolean requiredVersion = ReqConfirmTimetableTeacherLoadPolicyDTO.class
                .getMethod("expectedVersion").isAnnotationPresent(NotNull.class);

        Assertions.assertTrue(validBody && requiredPolicy && requiredVersion,
                "Policy ID and expected version must be validated request fields");
    }
}
