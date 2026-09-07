package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals" })
class SemesterNotificationTemplateServiceTest {

    private SemesterNotificationTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new SemesterNotificationTemplateService();
    }

    @Test
    void buildsAcademicOfficeSubjectAndBody() {
        String subject = templateService.buildAcademicOfficeSubject("Học kỳ 1", "t-7d");
        Assertions.assertTrue(subject.contains("Học kỳ 1"), "subject should contain semester name");
        Assertions.assertTrue(subject.contains("t-7d"), "subject should contain checkpoint code");

        SemesterCompletenessSummaryDTO summary = new SemesterCompletenessSummaryDTO(
                false, 1, 0, 0, 5, 2, 1, 1, List.of("Issue 1", "Issue 2"));
        String body = templateService.buildAcademicOfficeBody("Học kỳ 1", "t-7d", summary, summary.details());
        Assertions.assertTrue(body.contains("Ban Giám hiệu"), "body should contain greeting");
        Assertions.assertTrue(body.contains("Issue 1"), "body should contain issue 1");
        Assertions.assertTrue(body.contains("Issue 2"), "body should contain issue 2");
        Assertions.assertTrue(body.contains("Sổ điểm chưa công bố: 1"), "body should contain count");
    }

    @Test
    void buildsTeacherSubjectsAndBody() {
        String subjSubject = templateService.buildSubjectTeacherSubject("Học kỳ 1", "Toán", "10A1");
        Assertions.assertEquals("[Nhắc nhở nhập điểm] Môn Toán - Lớp 10A1 - Học kỳ Học kỳ 1",
                subjSubject, "subject teacher email subject should match");

        String hrSubject = templateService.buildHomeroomTeacherSubject("Học kỳ 1", "10A1");
        Assertions.assertEquals("[Báo cáo tiến độ điểm lớp 10A1] Học kỳ Học kỳ 1",
                hrSubject, "homeroom teacher email subject should match");

        String body = templateService.buildTeacherBody(
                "Nguyễn Văn A", "Học kỳ 1", "t-7d", "Giáo viên bộ môn", List.of("Chưa nhập điểm miệng"));
        Assertions.assertTrue(body.contains("Nguyễn Văn A"), "body should contain teacher name");
        Assertions.assertTrue(body.contains("Chưa nhập điểm miệng"), "body should contain issue detail");
    }
}
