package com.JavaTraining.BaiTap_RS.attendance.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqAttendanceHistoryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResStudentAttendanceHistoryDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import org.springframework.stereotype.Component;

@Component
public class AttendanceHistoryResponseMapper {

    public ResStudentAttendanceHistoryDTO.Item toItem(
            CalendarDay day,
            AttendanceSessionPeriod period,
            SchoolClass schoolClass,
            AttendanceRecord record) {
        String exceptionStatus = record == null ? null : record.getStatus().name();
        return new ResStudentAttendanceHistoryDTO.Item(
                day.getCalendarDate(), period, schoolClass.getId(), schoolClass.getClassName(),
                status(record), record == null ? null : record.getId(), exceptionStatus,
                record == null ? null : record.getNote());
    }

    public ResStudentAttendanceHistoryDTO.Summary summarize(List<ResStudentAttendanceHistoryDTO.Item> items) {
        return new ResStudentAttendanceHistoryDTO.Summary(
                items.size(), count(items, "PRESENT"), count(items, "EXCUSED_ABSENCE"),
                count(items, "UNEXCUSED_ABSENCE"), count(items, "LATE"), count(items, "EARLY_LEAVE"));
    }

    public ResStudentAttendanceHistoryDTO page(
            List<ResStudentAttendanceHistoryDTO.Item> items,
            ResStudentAttendanceHistoryDTO.Summary summary,
            int page,
            int size) {
        int start = Math.min(page * size, items.size());
        int end = Math.min(start + size, items.size());
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / size);
        return new ResStudentAttendanceHistoryDTO(
                items.subList(start, end), summary, page, size, items.size(), totalPages);
    }

    public ResStudentAttendanceHistoryDTO emptyResponse(ReqAttendanceHistoryQuery query) {
        return new ResStudentAttendanceHistoryDTO(
                List.of(), new ResStudentAttendanceHistoryDTO.Summary(0, 0, 0, 0, 0, 0),
                query.resolvedPage(), query.resolvedSize(), 0, 0);
    }

    private String status(AttendanceRecord record) {
        if (record == null) {
            return "PRESENT";
        }
        return switch (record.getStatus()) {
            case EXCUSED -> "EXCUSED_ABSENCE";
            case ABSENT -> "UNEXCUSED_ABSENCE";
            case LATE -> "LATE";
            case EARLY_LEAVE -> "EARLY_LEAVE";
        };
    }

    private long count(List<ResStudentAttendanceHistoryDTO.Item> items, String status) {
        return items.stream().filter(item -> status.equals(item.status())).count();
    }
}
