package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentTermTranscriptRepository extends JpaRepository<StudentTermTranscript, Long> {

    Optional<StudentTermTranscript> findByAnnualTranscriptIdAndSemesterId(
            Long annualTranscriptId, Long semesterId);
}
