package com.JavaTraining.BaiTap_RS.library.circulation.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryBatchRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryBatchRunRepository extends JpaRepository<LibraryBatchRun, Long> {

    @Query("select r from LibraryBatchRun r order by r.cycle.startedAt desc")
    List<LibraryBatchRun> findOrderedByStartedAt(Pageable pageable);

    @Query("select r from LibraryBatchRun r where r.jobName = :jobName and r.runDate = :runDate "
            + "and r.actorUserId = :actorUserId and r.status = :status order by r.cycle.startedAt desc")
    List<LibraryBatchRun> findMatchingRunsOrderedByStartedAt(@Param("jobName") String jobName,
            @Param("runDate") java.time.LocalDate runDate, @Param("actorUserId") Long actorUserId,
            @Param("status") String status, Pageable pageable);
}
