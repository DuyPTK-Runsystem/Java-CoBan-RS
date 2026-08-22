package com.JavaTraining.BaiTap_RS.teacher.repository;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByTeacherCode(String teacherCode);

    boolean existsByTeacherCodeAndIdNot(String teacherCode, Long id);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndIdNot(Long userId, Long id);

    Optional<Teacher> findByUserId(Long userId);

    List<Teacher> findAllByStatusOrderByTeacherCodeAsc(TeacherStatus status);

    List<Teacher> findAllByOrderByTeacherCodeAsc();
}
