package com.JavaTraining.BaiTap_RS.notification.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationReceiptDTO;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationService;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v3/notifications")
public class NotificationController {

    private static final String ROLE_ADMIN_OR_ACADEMIC_OFFICE = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize(ROLE_ADMIN_OR_ACADEMIC_OFFICE)
    @ApiMessage("Tạo dự thảo thông báo")
    public ResponseEntity<ResNotificationDTO> create(
            @Valid @RequestBody ReqCreateNotificationDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ResNotificationDTO response = notificationService.createDraft(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/inbox")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Lấy danh sách thông báo đến (inbox)")
    public ResultPaginationDTO<ResNotificationDTO> inbox(
            @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(name = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(name = "size", defaultValue = "20") @Positive @Max(2000) int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.getInbox(principal.getId(), unreadOnly, PageRequest.of(page, size));
    }

    @GetMapping("/manage")
    @PreAuthorize(ROLE_ADMIN_OR_ACADEMIC_OFFICE)
    @ApiMessage("Lấy danh sách thông báo quản lý")
    public ResultPaginationDTO<ResNotificationDTO> manage(
            @RequestParam(name = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(name = "size", defaultValue = "20") @Positive @Max(2000) int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.getManagedNotifications(principal.getId(), PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy chi tiết thông báo")
    @PreAuthorize("isAuthenticated()")
    public ResNotificationDTO detail(
            @PathVariable("id") @Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isManager = principal.getRoleCodes().contains("ADMIN")
                || principal.getRoleCodes().contains("ACADEMIC_OFFICE");
        return notificationService.getNotificationDetail(id, principal.getId(), isManager);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize(ROLE_ADMIN_OR_ACADEMIC_OFFICE)
    @ApiMessage("Xuất bản thông báo")
    public ResNotificationDTO publish(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody(required = false) ReqPublishNotificationDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.publish(id, request, principal.getId());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(ROLE_ADMIN_OR_ACADEMIC_OFFICE)
    @ApiMessage("Hủy thông báo")
    public ResNotificationDTO cancel(
            @PathVariable("id") @Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.cancel(id, principal.getId());
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @ApiMessage("Đánh dấu thông báo đã đọc")
    public ResNotificationReceiptDTO markRead(
            @PathVariable("id") @Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.markAsRead(id, principal.getId());
    }
}
