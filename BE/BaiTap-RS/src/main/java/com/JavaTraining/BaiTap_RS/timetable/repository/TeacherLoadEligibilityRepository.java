package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadEligibility;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadEligibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherLoadEligibilityRepository extends JpaRepository<TeacherLoadEligibility, Long> {

        List<TeacherLoadEligibility> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

        List<TeacherLoadEligibility> findByTeacherIdAndStatus(Long teacherId, TeacherLoadEligibilityStatus status);

        @Query("SELECT e FROM TeacherLoadEligibility e WHERE "
                        + "e.teacherId = :teacherId AND "
                        + "e.status = 'ACTIVE' AND "
                        + "e.validFrom <= :date AND e.validTo >= :date")
        List<TeacherLoadEligibility> findActiveByTeacherIdAndDate(
                        @Param("teacherId") Long teacherId,
                        @Param("date") LocalDate date);

        @Query("SELECT e FROM TeacherLoadEligibility e WHERE "
                        + "e.status = 'ACTIVE' AND "
                        + "e.validFrom <= :date AND e.validTo >= :date")
        List<TeacherLoadEligibility> findAllActiveByDate(@Param("date") LocalDate date);
}
