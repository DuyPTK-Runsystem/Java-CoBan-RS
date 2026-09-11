package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableHeadRepository extends JpaRepository<TimetableHead, Long> {

    Optional<TimetableHead> findBySemesterId(Long semesterId);

    boolean existsBySemesterId(Long semesterId);
}
