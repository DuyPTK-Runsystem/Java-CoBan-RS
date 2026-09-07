package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({
        "PMD.ConsecutiveLiteralAppends",
        "PMD.UseObjectForClearerAPI",
        "PMD.GuardLogStatement"
})
public class SemesterNotificationTemplateService {

    public String buildAcademicOfficeSubject(String semesterName, String checkpointCode) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterNotificationTemplateService.class,
                "SemesterNotificationTemplateService.buildAcademicOfficeSubject");
        return String.format("[Cảnh báo dữ liệu điểm] Học kỳ %s - Checkpoint %s",
                semesterName, checkpointCode);
    }

    public String buildAcademicOfficeBody(
            String semesterName,
            String checkpointCode,
            SemesterCompletenessSummaryDTO summary,
            List<String> details) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterNotificationTemplateService.class,
                        "SemesterNotificationTemplateService.buildAcademicOfficeBody");
        StringBuilder builder = new StringBuilder(512);
        builder.append("Kính gửi Ban Giám hiệu / Phòng Giáo vụ,\n\n")
                .append(String.format("Hệ thống thông báo tình trạng dữ liệu điểm của học kỳ [%s] tại mốc [%s]:\n",
                        semesterName, checkpointCode))
                .append("- Sổ điểm chưa công bố: ").append(summary.unpublishedScorebookCount()).append('\n')
                .append("- Thiếu cấu hình KTĐK: ").append(summary.missingKtdkCount()).append('\n')
                .append("- Lỗi cấu hình KTCK: ").append(summary.invalidKtckCount()).append('\n')
                .append("- Môn kỹ năng thiếu cột: ").append(summary.missingSkillColumnsCount()).append('\n')
                .append("- Điểm chưa nhập: ").append(summary.unenteredScoreCount()).append('\n')
                .append("- Học sinh chưa có điểm: ").append(summary.studentWithoutScoreDataCount()).append('\n')
                .append("- Yêu cầu sửa điểm chờ duyệt: ")
                .append(summary.pendingScoreChangeRequestCount()).append("\n\n");

        if (details != null && !details.isEmpty()) {
            builder.append("Chi tiết các vấn đề cần xử lý:\n");
            for (String item : details) {
                builder.append(" • ").append(item).append('\n');
            }
        }
        builder.append("\nTrân trọng,\nHệ thống Quản lý Đào tạo");
        return builder.toString();
    }

    public String buildSubjectTeacherSubject(String semesterName, String subjectName, String className) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterNotificationTemplateService.class,
                "SemesterNotificationTemplateService.buildSubjectTeacherSubject");
        return String.format("[Nhắc nhở nhập điểm] Môn %s - Lớp %s - Học kỳ %s",
                subjectName, className, semesterName);
    }

    public String buildHomeroomTeacherSubject(String semesterName, String className) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterNotificationTemplateService.class,
                "SemesterNotificationTemplateService.buildHomeroomTeacherSubject");
        return String.format("[Báo cáo tiến độ điểm lớp %s] Học kỳ %s",
                className, semesterName);
    }

    public String buildTeacherBody(
            String teacherName,
            String semesterName,
            String checkpointCode,
            String roleDescription,
            List<String> scopedDetails) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterNotificationTemplateService.class,
                        "SemesterNotificationTemplateService.buildTeacherBody");
        StringBuilder builder = new StringBuilder(512);
        builder.append("Kính gửi Thầy/Cô ").append(teacherName != null ? teacherName : "").append(",\n\n")
                .append(String.format(
                        "Hệ thống gửi thông báo tiến độ nhập điểm học kỳ [%s] (mốc [%s]) đối với vai trò [%s]:\n\n",
                        semesterName, checkpointCode, roleDescription));

        if (scopedDetails != null && !scopedDetails.isEmpty()) {
            builder.append("Danh sách các mục cần kiểm tra hoặc hoàn thiện:\n");
            for (String detail : scopedDetails) {
                builder.append(" • ").append(detail).append('\n');
            }
        } else {
            builder.append("Không có ghi nhận thiếu sót nào trong phạm vi quản lý của Thầy/Cô.\n");
        }

        builder.append("\nKính đề nghị Thầy/Cô sớm hoàn thành để đảm bảo tiến độ khóa học kỳ.\n")
                .append("Trân trọng,\nPhòng Giáo vụ");
        return builder.toString();
    }
}
