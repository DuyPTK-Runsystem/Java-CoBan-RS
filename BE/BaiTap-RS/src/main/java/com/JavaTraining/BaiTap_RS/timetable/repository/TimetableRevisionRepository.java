package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableRevisionRepository extends JpaRepository<TimetableRevision, Long> {

    List<TimetableRevision> findByTimetableIdOrderByRevisionNumberDesc(Long timetableId);

    Optional<TimetableRevision> findByTimetableIdAndRevisionNumber(Long timetableId, Integer revisionNumber);

    @Query("SELECT MAX(r.revisionNumber) FROM TimetableRevision r WHERE r.timetableId = :timetableId")
    Integer findMaxRevisionNumber(@Param("timetableId") Long timetableId);

    Page<TimetableRevision> findBySemesterId(Long semesterId, Pageable pageable);

    List<TimetableRevision> findBySemesterIdAndStatus(Long semesterId, TimetableRevisionStatus status);
}
