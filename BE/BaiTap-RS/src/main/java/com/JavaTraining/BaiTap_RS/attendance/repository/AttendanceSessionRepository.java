package com.JavaTraining.BaiTap_RS.attendance.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findByClassIdAndAttendanceDateAndSessionPeriod(
            Long classId,
            LocalDate attendanceDate,
            AttendanceSessionPeriod sessionPeriod);
}
