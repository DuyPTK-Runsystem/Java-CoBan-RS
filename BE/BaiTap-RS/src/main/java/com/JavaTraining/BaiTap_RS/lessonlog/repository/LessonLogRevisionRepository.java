package com.JavaTraining.BaiTap_RS.lessonlog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;

public interface LessonLogRevisionRepository extends JpaRepository<LessonLogRevision, Long> {

    Page<LessonLogRevision> findByEntryIdOrderByCreatedAtDesc(Long entryId, Pageable pageable);

    Page<LessonLogRevision> findByWeeklyReviewIdOrderByCreatedAtDesc(Long weeklyReviewId, Pageable pageable);

    Page<LessonLogRevision> findByPolicyIdOrderByCreatedAtDesc(Long policyId, Pageable pageable);

    @Query("select revision from LessonLogRevision revision where revision.policyId is not null order by revision.createdAt desc")
    Page<LessonLogRevision> findPolicyRevisions(Pageable pageable);
}
