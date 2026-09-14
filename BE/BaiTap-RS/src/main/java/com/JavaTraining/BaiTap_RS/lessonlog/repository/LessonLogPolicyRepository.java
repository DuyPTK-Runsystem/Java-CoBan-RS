package com.JavaTraining.BaiTap_RS.lessonlog.repository;
import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
public interface LessonLogPolicyRepository extends JpaRepository<LessonLogPolicy,Long> {
 Optional<LessonLogPolicy> findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate date);
 Optional<LessonLogPolicy> findTopByOrderByPolicyVersionDesc();
}
