package com.JavaTraining.BaiTap_RS.lessonlog.service;

public record LessonLogAuditChange(String action, String reason, String beforeState, String afterState) {
}
