package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
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

    List<ClassSubject> findAllByClassIdInAndSemesterIdIn(Collection<Long> classIds, Collection<Long> semesterIds);

    List<ClassSubject> findAllBySemesterId(Long semesterId);

    List<ClassSubject> findAllBySemesterIdAndStatus(Long semesterId, ClassSubjectStatus status);

    @Query("""
            select case when count(classSubject) > 0 then true else false end
            from ClassSubject classSubject
            join SchoolClass schoolClass on schoolClass.id = classSubject.classId
            where classSubject.subjectId = :subjectId
              and classSubject.semesterId = :semesterId
              and ((:scopeType = com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope.GRADE
                    and schoolClass.gradeLevelId = :gradeLevelId)
                or (:scopeType = com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope.CLASS
                    and classSubject.classId = :classId))
            """)
    boolean existsByApplicabilityTarget(
            @Param("subjectId") Long subjectId,
            @Param("semesterId") Long semesterId,
            @Param("scopeType") ApplicationScope scopeType,
            @Param("gradeLevelId") Long gradeLevelId,
            @Param("classId") Long classId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select classSubject from ClassSubject classSubject where classSubject.id = :id")
    Optional<ClassSubject> findByIdForUpdate(@Param("id") Long id);
}
