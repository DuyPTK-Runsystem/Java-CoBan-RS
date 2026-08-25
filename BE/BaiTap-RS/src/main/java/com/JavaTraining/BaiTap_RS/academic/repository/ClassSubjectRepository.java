package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    boolean existsByClassIdAndSubjectIdAndSemesterId(Long classId, Long subjectId, Long semesterId);

    boolean existsBySubjectId(Long subjectId);

    List<ClassSubject> findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(Long classId, Long semesterId);

    List<ClassSubject> findAllBySemesterId(Long semesterId);

    List<ClassSubject> findAllBySemesterIdAndStatus(Long semesterId, ClassSubjectStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select classSubject from ClassSubject classSubject where classSubject.id = :id")
    Optional<ClassSubject> findByIdForUpdate(@Param("id") Long id);
}
