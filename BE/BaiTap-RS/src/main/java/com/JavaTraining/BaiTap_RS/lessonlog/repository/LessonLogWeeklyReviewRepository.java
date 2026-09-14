package com.JavaTraining.BaiTap_RS.lessonlog.repository;
import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
public interface LessonLogWeeklyReviewRepository extends JpaRepository<LessonLogWeeklyReview,Long> {
 Optional<LessonLogWeeklyReview> findByClassIdAndSemesterIdAndWeekStart(Long c,Long s,LocalDate w);
}
