package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableHeadRepository extends JpaRepository<TimetableHead, Long> {

    Optional<TimetableHead> findBySemesterId(Long semesterId);

    /**
     * Locks the single semester head before a workflow reads or changes the
     * revision pointer. Callers acquire this lock before resolving the rest of
     * the workflow and keep it until the surrounding transaction completes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from TimetableHead h where h.id = :id and h.semesterId = :semesterId")
    Optional<TimetableHead> findByIdAndSemesterIdForUpdate(@Param("id") Long id,
            @Param("semesterId") Long semesterId);

    boolean existsBySemesterId(Long semesterId);
}
