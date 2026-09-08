package com.JavaTraining.BaiTap_RS.student.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class StudentDeletionGuardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public boolean existsByStudentId(Long studentId) {
        Long count = entityManager.createQuery(
                "select count(enrollment) from StudentYearEnrollment enrollment where enrollment.studentId = :studentId",
                Long.class)
                .setParameter("studentId", studentId)
                .getSingleResult();
        return count > 0;
    }
}
