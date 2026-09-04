package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSubjectAnnualResultRepository extends JpaRepository<StudentSubjectAnnualResult, Long> {

    List<StudentSubjectAnnualResult> findAllByAnnualTranscriptIdOrderBySubjectIdAsc(Long annualTranscriptId);

    List<StudentSubjectAnnualResult> findAllBySubjectIdOrderByAnnualTranscriptIdAsc(Long subjectId);

    Optional<StudentSubjectAnnualResult> findByAnnualTranscriptIdAndSubjectId(Long annualTranscriptId, Long subjectId);

    List<StudentSubjectAnnualResult> findAllByAnnualTranscriptIdInOrderBySubjectIdAsc(
            java.util.Collection<Long> annualTranscriptIds);
}
