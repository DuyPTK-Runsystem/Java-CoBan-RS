package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findByRevisionId(Long revisionId);

    void deleteByRevisionId(Long revisionId);

    boolean existsByFunctionalRoomId(Long functionalRoomId);

    List<TimetableEntry> findByRevisionIdIn(List<Long> revisionIds);
}
