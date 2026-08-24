package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentAnnualTranscriptRepository extends JpaRepository<StudentAnnualTranscript, Long> {

        Optional<StudentAnnualTranscript> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT t
                        FROM StudentAnnualTranscript t
                        WHERE t.studentId = :studentId AND t.academicYearId = :academicYearId
                        """)
        Optional<StudentAnnualTranscript> findForUpdate(
                        @Param("studentId") Long studentId,
                        @Param("academicYearId") Long academicYearId);
}
