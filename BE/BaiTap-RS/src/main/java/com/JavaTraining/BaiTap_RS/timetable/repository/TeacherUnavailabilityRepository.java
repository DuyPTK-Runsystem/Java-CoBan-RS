package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailability;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherUnavailabilityRepository extends JpaRepository<TeacherUnavailability, Long> {

        List<TeacherUnavailability> findBySemesterIdOrderByCreatedAtDesc(Long semesterId);

        List<TeacherUnavailability> findBySemesterIdAndTeacherIdOrderByCreatedAtDesc(Long semesterId, Long teacherId);

        List<TeacherUnavailability> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

        @Query("SELECT u FROM TeacherUnavailability u WHERE "
                        + "u.semesterId = :semesterId AND "
                        + "(:teacherId IS NULL OR u.teacherId = :teacherId) AND "
                        + "(:status IS NULL OR u.status = :status) AND "
                        + "u.validFrom <= :to AND u.validTo >= :from "
                        + "ORDER BY u.createdAt DESC")
        List<TeacherUnavailability> searchUnavailabilities(
                        @Param("semesterId") Long semesterId,
                        @Param("teacherId") Long teacherId,
                        @Param("status") TeacherUnavailabilityStatus status,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to);

        @Query("SELECT u FROM TeacherUnavailability u WHERE "
                        + "u.semesterId = :semesterId AND "
                        + "u.status = 'APPROVED' AND "
                        + "u.validFrom <= :to AND u.validTo >= :from")
        List<TeacherUnavailability> findApprovedInSemester(
                        @Param("semesterId") Long semesterId,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to);
}
