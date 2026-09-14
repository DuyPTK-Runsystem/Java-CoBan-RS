package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response;
import java.time.*; import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.*;
public record LessonLogEntryResponse(Long entryId,Long timetableEntryId,Long classId,Long semesterId,LocalDate lessonDate,String session,Integer periodIndex,String title,String content,String completionStatus,String grade,Integer presentCount,Integer absentCount,String comments,String absentStudentNotes,String homework,LessonLogStatus status,Long version,LocalDateTime expiresAt) {}
