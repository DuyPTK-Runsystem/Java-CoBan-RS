package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSubjectTermResultRepository extends JpaRepository<StudentSubjectTermResult, Long> {

    List<StudentSubjectTermResult> findAllByTermTranscriptIdOrderBySubjectIdAsc(Long termTranscriptId);

    List<StudentSubjectTermResult> findAllBySubjectIdOrderByTermTranscriptIdAsc(Long subjectId);

    Optional<StudentSubjectTermResult> findByTermTranscriptIdAndSubjectId(Long termTranscriptId, Long subjectId);
}
