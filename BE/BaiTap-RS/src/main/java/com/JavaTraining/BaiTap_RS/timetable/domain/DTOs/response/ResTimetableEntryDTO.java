package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;

public record ResTimetableEntryDTO(
                Long entryId,
                Long revisionId,
                Long assignmentId,
                Long periodId,
                Long functionalRoomId,
                String roomCode,
                String roomName,
                Long classId,
                String className,
                Long subjectId,
                String subjectName,
                Long teacherId,
                String teacherName,
                LocalDate validFrom,
                LocalDate validTo,
                Integer dayOfWeek,
                SessionType session,
                Integer periodIndex,
                LocalTime startTime,
                LocalTime endTime) {
}
