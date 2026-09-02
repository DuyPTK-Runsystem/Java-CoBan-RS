package com.JavaTraining.BaiTap_RS.calendar.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class CalendarAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public CalendarAuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void writeAudit(String action, CalendarDay before, CalendarDay after) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarAuditService.class,
                "CalendarAuditService.writeAudit");
        CalendarDay target = after == null ? before : after;
        auditLogRepository.save(new AuditLog(
                AuditContext.currentUserId(),
                action,
                "calendar_day",
                target == null || target.getId() == null ? "pending" : target.getId().toString(),
                before == null ? null : auditJson(dayData(before)),
                after == null ? null : auditJson(dayData(after)),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    public Map<String, Object> dayData(CalendarDay day) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarAuditService.class,
                "CalendarAuditService.dayData");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", day.getId());
        data.put("academicYearId", day.getAcademicYearId());
        data.put("semesterId", day.getSemesterId());
        data.put("calendarDate", day.getCalendarDate());
        data.put("dayType", day.getDayType().name());
        data.put("reason", day.getReason());
        return data;
    }

    public CalendarDay copyDay(CalendarDay source) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarAuditService.class,
                "CalendarAuditService.copyDay");
        CalendarDay copy = new CalendarDay(
                source.getAcademicYearId(),
                source.getSemesterId(),
                source.getCalendarDate(),
                source.getDayType(),
                source.getReason(),
                source.getConfiguredBy());
        copy.setId(source.getId());
        copy.setConfiguredAt(source.getConfiguredAt());
        copy.setUpdatedBy(source.getUpdatedBy());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    public void writeSessionAudit(String action, CalendarSession before, CalendarSession after) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarAuditService.class,
                "CalendarAuditService.writeSessionAudit");
        CalendarSession target = after == null ? before : after;
        auditLogRepository.save(new AuditLog(
                AuditContext.currentUserId(),
                action,
                "calendar_session",
                target == null || target.getId() == null ? "pending" : target.getId().toString(),
                before == null ? null : auditJson(sessionData(before)),
                after == null ? null : auditJson(sessionData(after)),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    public CalendarSession copySession(CalendarSession source) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarAuditService.class,
                "CalendarAuditService.copySession");
        CalendarSession copy = new CalendarSession(
                source.getCalendarDayId(),
                source.getSessionPeriod(),
                source.getSessionStatus(),
                source.getReason(),
                source.getConfiguredBy());
        copy.setId(source.getId());
        copy.setConfiguredAt(source.getConfiguredAt());
        copy.setUpdatedBy(source.getUpdatedBy());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    public Map<String, Object> sessionData(CalendarSession session) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarAuditService.class,
                "CalendarAuditService.sessionData");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", session.getId());
        data.put("calendarDayId", session.getCalendarDayId());
        data.put("sessionPeriod", session.getSessionPeriod().name());
        data.put("sessionStatus", session.getSessionStatus().name());
        data.put("reason", session.getReason());
        return data;
    }

    public String auditJson(Map<String, Object> data) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarAuditService.class,
                "CalendarAuditService.auditJson");
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo dữ liệu audit", exception);
        }
    }
}
