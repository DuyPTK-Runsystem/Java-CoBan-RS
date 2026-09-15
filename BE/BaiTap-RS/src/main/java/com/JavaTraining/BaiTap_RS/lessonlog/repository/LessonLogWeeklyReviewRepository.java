package com.JavaTraining.BaiTap_RS.lessonlog.repository;
import java.time.LocalDate; import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
public interface LessonLogWeeklyReviewRepository extends JpaRepository<LessonLogWeeklyReview,Long> {
 Optional<LessonLogWeeklyReview> findByClassIdAndSemesterIdAndWeekStart(Long c,Long s,LocalDate w);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select review from LessonLogWeeklyReview review where review.classId = :classId and review.semesterId = :semesterId and review.weekStart = :weekStart")
 Optional<LessonLogWeeklyReview> findByClassIdAndSemesterIdAndWeekStartForUpdate(
         @Param("classId") Long classId, @Param("semesterId") Long semesterId, @Param("weekStart") LocalDate weekStart);
 List<LessonLogWeeklyReview> findByClassIdAndSemesterId(Long classId, Long semesterId);
 List<LessonLogWeeklyReview> findByClassId(Long classId);
}
