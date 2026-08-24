package com.JavaTraining.BaiTap_RS.scorebook.repository;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillWeightConfigRepository extends JpaRepository<SkillWeightConfig, Long> {

    Optional<SkillWeightConfig> findByScorebookId(Long scorebookId);
}
