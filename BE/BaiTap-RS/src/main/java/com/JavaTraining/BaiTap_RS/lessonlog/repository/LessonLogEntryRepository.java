package com.JavaTraining.BaiTap_RS.lessonlog.repository;
import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
public interface LessonLogEntryRepository extends JpaRepository<LessonLogEntry,Long> {
 Optional<LessonLogEntry> findByClassIdAndLessonDateAndSessionAndPeriodIndex(Long c,LocalDate d,String s,Integer p);
 List<LessonLogEntry> findByAssignedTeacherIdAndLessonDate(Long t,LocalDate d);
 List<LessonLogEntry> findByClassIdAndSemesterIdAndLessonDateBetweenOrderByLessonDateAscPeriodIndexAsc(Long c,Long s,LocalDate from,LocalDate to);
 List<LessonLogEntry> findByAssignedTeacherIdAndLessonDateBetweenOrderByLessonDateAscPeriodIndexAsc(Long t,LocalDate from,LocalDate to);
 boolean existsByTimetableRevisionId(Long revisionId);
 boolean existsBySemesterIdAndLessonDate(Long semesterId, LocalDate lessonDate);
 boolean existsBySemesterIdAndLessonDateBetween(Long semesterId, LocalDate from, LocalDate to);
}
