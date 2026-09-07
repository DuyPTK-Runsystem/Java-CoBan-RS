package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExam;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RetakeExamRepository extends JpaRepository<RetakeExam, Long>, JpaSpecificationExecutor<RetakeExam> {

        Optional<RetakeExam> findByStudentIdAndAcademicYearIdAndSubjectId(
                        Long studentId,
                        Long academicYearId,
                        Long subjectId);

        List<RetakeExam> findAllByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

        List<RetakeExam> findAllByStudentIdAndAcademicYearIdAndStatus(
                        Long studentId,
                        Long academicYearId,
                        RetakeExamStatus status);

        Optional<RetakeExam> findByStudentIdAndAcademicYearIdAndSubjectIdAndStatus(
                        Long studentId,
                        Long academicYearId,
                        Long subjectId,
                        RetakeExamStatus status);
}
