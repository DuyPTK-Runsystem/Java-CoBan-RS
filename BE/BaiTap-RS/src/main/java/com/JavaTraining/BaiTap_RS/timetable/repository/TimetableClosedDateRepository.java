package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableClosedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableClosedDateRepository extends JpaRepository<TimetableClosedDate, Long> {

    List<TimetableClosedDate> findByCalendarIdOrderByClosedDateAsc(Long calendarId);

    boolean existsByCalendarIdAndClosedDate(Long calendarId, LocalDate closedDate);

    void deleteByCalendarId(Long calendarId);
}
