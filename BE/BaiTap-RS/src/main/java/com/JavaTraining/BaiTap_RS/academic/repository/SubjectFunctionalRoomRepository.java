package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectFunctionalRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectFunctionalRoomRepository extends JpaRepository<SubjectFunctionalRoom, Long> {

    List<SubjectFunctionalRoom> findBySubjectId(Long subjectId);

    List<SubjectFunctionalRoom> findByFunctionalRoomId(Long functionalRoomId);

    boolean existsBySubjectId(Long subjectId);

    boolean existsByFunctionalRoomId(Long functionalRoomId);

    boolean existsBySubjectIdAndFunctionalRoomId(Long subjectId, Long functionalRoomId);

    void deleteBySubjectId(Long subjectId);
}
