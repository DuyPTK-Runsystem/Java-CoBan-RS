package com.JavaTraining.BaiTap_RS.academic.repository;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeLevelRepository extends JpaRepository<GradeLevel, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByLevel(Integer gradeLevel);

    boolean existsByLevelAndIdNot(Integer gradeLevel, Long id);

    boolean existsByNextGradeId(Long gradeLevelId);
}
