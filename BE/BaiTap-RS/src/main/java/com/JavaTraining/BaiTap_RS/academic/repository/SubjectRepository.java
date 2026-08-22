package com.JavaTraining.BaiTap_RS.academic.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Subject> findAllByStatusOrderByCodeAsc(SubjectStatus status);

    List<Subject> findAllByOrderByCodeAsc();
}
