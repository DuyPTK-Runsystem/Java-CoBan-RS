package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherLoadPolicyRepository extends JpaRepository<TeacherLoadPolicy, Long> {

    Optional<TeacherLoadPolicy> findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus status);

    Optional<TeacherLoadPolicy> findByVersion(String version);

    boolean existsByVersion(String version);

    Page<TeacherLoadPolicy> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
