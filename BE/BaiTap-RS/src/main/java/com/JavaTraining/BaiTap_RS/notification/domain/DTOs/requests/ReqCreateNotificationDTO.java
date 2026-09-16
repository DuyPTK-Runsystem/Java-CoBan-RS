package com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqCreateNotificationDTO {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 4000, message = "Nội dung tối đa 4000 ký tự")
    private String body;

    @NotNull(message = "Loại đối tượng nhận thông báo không được để trống")
    private NotificationAudienceType audienceType;

    @Size(max = 255, message = "Mã tham chiếu đối tượng tối đa 255 ký tự")
    private String targetReference;

    private List<Long> recipientUserIds;

    private NotificationChannel channel;

    private LocalDateTime publishAt;

    private LocalDateTime expiresAt;

    @Size(max = 100, message = "Idempotency key tối đa 100 ký tự")
    private String idempotencyKey;

    @NotBlank(message = "School scope không được để trống")
    @Size(max = 100, message = "School scope tối đa 100 ký tự")
    private String schoolScope;

    @AssertTrue(message = "Thời điểm hết hạn phải sau thời điểm phát hành")
    public boolean hasValidExpiryWindow() {
        return publishAt == null || expiresAt == null || expiresAt.isAfter(publishAt);
    }

    @AssertTrue(message = "Notification v3 hiện chỉ hỗ trợ publish ngay, không hỗ trợ lên lịch")
    public boolean hasImmediatePublishOnly() {
        return publishAt == null;
    }
}
