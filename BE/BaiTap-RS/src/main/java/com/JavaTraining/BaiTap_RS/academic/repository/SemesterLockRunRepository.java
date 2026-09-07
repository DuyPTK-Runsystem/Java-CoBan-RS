package com.JavaTraining.BaiTap_RS.academic.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterLockRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SemesterLockRunRepository extends JpaRepository<SemesterLockRun, Long> {

    Optional<SemesterLockRun> findFirstByBusinessDateOrderByIdDesc(LocalDate businessDate);
}
