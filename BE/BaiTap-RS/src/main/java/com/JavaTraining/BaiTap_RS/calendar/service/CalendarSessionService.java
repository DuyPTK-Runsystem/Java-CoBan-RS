package com.JavaTraining.BaiTap_RS.calendar.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.requests.ReqCalendarSessionDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarSessionRepository;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class CalendarSessionService {

    private final CalendarSessionRepository sessionRepository;
    private final CalendarAuditService auditService;

    public CalendarSessionService(
            CalendarSessionRepository sessionRepository,
            CalendarAuditService auditService) {
        this.sessionRepository = sessionRepository;
        this.auditService = auditService;
    }

    public void upsertSessions(CalendarDay day, List<ReqCalendarSessionDTO> requests, Long userId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarSessionService.class,
                "CalendarSessionService.upsertSessions");
        Map<CalendarSessionPeriod, ReqCalendarSessionDTO> requested = requests.stream()
                .collect(Collectors.toMap(
                        ReqCalendarSessionDTO::sessionPeriod,
                        Function.identity(),
                        (first, second) -> first,
                        () -> new EnumMap<>(CalendarSessionPeriod.class)));
        for (CalendarSessionPeriod period : CalendarSessionPeriod.values()) {
            CalendarSession session = sessionRepository.findByCalendarDayIdAndSessionPeriod(day.getId(), period)
                    .orElse(null);
            ReqCalendarSessionDTO request = requested.get(period);
            if (session == null && request != null) {
                createSession(day, request, userId);
            } else if (session != null) {
                CalendarSession before = auditService.copySession(session);
                updateSession(session, request, userId);
                auditService.writeSessionAudit("CALENDAR_SESSION_UPDATED", before, session);
            }
        }
    }

    public void ensureScheduled(CalendarDay day, CalendarSessionPeriod period, Long userId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarSessionService.class,
                "CalendarSessionService.ensureScheduled",
                "ensuring dayId={}, period={}, status={}",
                day.getId(), period, CalendarSessionStatus.SCHEDULED);
        if (sessionRepository.findByCalendarDayIdAndSessionPeriod(day.getId(), period).isEmpty()) {
            createSession(day, new ReqCalendarSessionDTO(
                    period,
                    CalendarSessionStatus.SCHEDULED,
                    null), userId);
        }
    }

    private void createSession(CalendarDay day, ReqCalendarSessionDTO request, Long userId) {
        CalendarSession session = sessionRepository.save(new CalendarSession(
                day.getId(),
                request.sessionPeriod(),
                request.sessionStatus(),
                request.reason(),
                userId));
        auditService.writeSessionAudit("CALENDAR_SESSION_CREATED", null, session);
    }

    private void updateSession(CalendarSession session, ReqCalendarSessionDTO request, Long userId) {
        CalendarSessionStatus status = request == null
                ? CalendarSessionStatus.NO_CLASS
                : request.sessionStatus();
        String reason = request == null ? "Không có lịch buổi học" : request.reason();
        session.update(status, reason, userId);
    }
}
