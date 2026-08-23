package com.JavaTraining.BaiTap_RS.calendar.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarSessionRepository extends JpaRepository<CalendarSession, Long> {

    List<CalendarSession> findAllByCalendarDayIdOrderBySessionPeriodAsc(Long calendarDayId);

    Optional<CalendarSession> findByCalendarDayIdAndSessionPeriod(
            Long calendarDayId,
            CalendarSessionPeriod sessionPeriod);

    boolean existsByCalendarDayIdAndSessionPeriodAndSessionStatus(
            Long calendarDayId,
            CalendarSessionPeriod sessionPeriod,
            CalendarSessionStatus sessionStatus);
}
