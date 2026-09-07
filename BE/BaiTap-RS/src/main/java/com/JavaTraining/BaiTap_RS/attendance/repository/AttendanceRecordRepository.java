package com.JavaTraining.BaiTap_RS.attendance.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<AttendanceRecord> findAllBySessionIdAndStudentIdIn(Long sessionId, Collection<Long> studentIds);

    List<AttendanceRecord> findAllBySessionIdIn(Collection<Long> sessionIds);
}
