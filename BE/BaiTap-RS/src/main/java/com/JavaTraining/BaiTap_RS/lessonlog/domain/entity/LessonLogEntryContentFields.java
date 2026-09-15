package com.JavaTraining.BaiTap_RS.lessonlog.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class LessonLogEntryContentFields extends LessonLogEntrySourceFields {
    private String title;
    @Column(columnDefinition = "TEXT") private String content;
    private String completionStatus;
    private String grade;
    private Integer presentCount;
    private Integer absentCount;
    private String comments;
    private String absentStudentNotes;
    private String homework;
}
