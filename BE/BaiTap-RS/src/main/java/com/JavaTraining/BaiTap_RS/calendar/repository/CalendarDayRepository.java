package com.JavaTraining.BaiTap_RS.calendar.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarDayRepository extends JpaRepository<CalendarDay, Long> {

    Optional<CalendarDay> findByAcademicYearIdAndCalendarDate(Long academicYearId, LocalDate calendarDate);

    List<CalendarDay> findAllBySemesterIdAndCalendarDateBetweenOrderByCalendarDateAsc(
            Long semesterId,
            LocalDate from,
            LocalDate to);

    List<CalendarDay> findAllByCalendarDateBetweenOrderByCalendarDateAsc(LocalDate from, LocalDate to);
}
