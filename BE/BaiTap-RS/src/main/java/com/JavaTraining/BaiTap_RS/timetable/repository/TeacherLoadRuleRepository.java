package com.JavaTraining.BaiTap_RS.timetable.repository;

import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherLoadRuleRepository extends JpaRepository<TeacherLoadRule, Long> {

    List<TeacherLoadRule> findAllByPolicyIdOrderByIdAsc(Long policyId);

    boolean existsByPolicyIdAndRuleCode(Long policyId, String ruleCode);
}
