package com.JavaTraining.BaiTap_RS.lessonlog.service;

public record LessonLogEntryContent(String title, String content, String completionStatus, String grade,
        Integer presentCount, Integer absentCount, String comments, String absentStudentNotes, String homework) {
}
