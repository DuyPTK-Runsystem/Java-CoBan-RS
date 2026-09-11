package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetablePeriodRepository extends JpaRepository<TimetablePeriod, Long> {

    List<TimetablePeriod> findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(Long calendarId);

    void deleteByCalendarId(Long calendarId);
}
