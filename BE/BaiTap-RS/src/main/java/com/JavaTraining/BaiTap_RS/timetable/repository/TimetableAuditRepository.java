package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableAuditRepository extends JpaRepository<TimetableAudit, Long> {

    List<TimetableAudit> findByTimetableIdOrderByCreatedAtDesc(Long timetableId);

    List<TimetableAudit> findByRevisionIdOrderByCreatedAtDesc(Long revisionId);
}
