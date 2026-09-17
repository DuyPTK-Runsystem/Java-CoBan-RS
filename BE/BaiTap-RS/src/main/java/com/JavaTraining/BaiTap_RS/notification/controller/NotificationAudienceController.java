package com.JavaTraining.BaiTap_RS.notification.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationClassAudienceDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationIndividualAudienceDTO;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationAudienceLookupService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v3/notifications/audiences")
public class NotificationAudienceController {

    private static final String ROLE_NOTIFICATION_SENDER =
            "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private static final String PAGE_PARAM = "page";
    private static final String SIZE_PARAM = "size";

    private final NotificationAudienceLookupService lookupService;

    public NotificationAudienceController(NotificationAudienceLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/classes")
    @PreAuthorize(ROLE_NOTIFICATION_SENDER)
    @ApiMessage("Lấy danh sách lớp đủ điều kiện nhận thông báo")
    public ResultPaginationDTO<ResNotificationClassAudienceDTO> classes(
            @RequestParam(name = "q", required = false) @Size(max = 100) String query,
            @RequestParam(name = PAGE_PARAM, defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(name = SIZE_PARAM, defaultValue = "20") @Positive @Max(200) int size) {
        return lookupService.findClasses(query, PageRequest.of(page, size));
    }

    @GetMapping("/individuals")
    @PreAuthorize(ROLE_NOTIFICATION_SENDER)
    @ApiMessage("Lấy danh sách người nhận đủ điều kiện")
    public ResultPaginationDTO<ResNotificationIndividualAudienceDTO> individuals(
            @RequestParam(name = "q", required = false) @Size(max = 100) String query,
            @RequestParam(name = "roleCode", required = false)
            @Pattern(regexp = "STUDENT|TEACHER|ACADEMIC_OFFICE|ADMIN") String roleCode,
            @RequestParam(name = "studentClassId", required = false) @Positive Long studentClassId,
            @RequestParam(name = "teacherClassId", required = false) @Positive Long teacherClassId,
            @RequestParam(name = PAGE_PARAM, defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(name = SIZE_PARAM, defaultValue = "20") @Positive @Max(200) int size) {
        return lookupService.findIndividuals(
                query, roleCode, studentClassId, teacherClassId, PageRequest.of(page, size));
    }
}
