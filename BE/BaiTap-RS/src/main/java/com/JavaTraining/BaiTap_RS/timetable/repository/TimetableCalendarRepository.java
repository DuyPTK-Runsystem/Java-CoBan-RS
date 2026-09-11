package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableCalendarRepository extends JpaRepository<TimetableCalendar, Long> {

    Optional<TimetableCalendar> findBySemesterId(Long semesterId);

    boolean existsBySemesterId(Long semesterId);
}
