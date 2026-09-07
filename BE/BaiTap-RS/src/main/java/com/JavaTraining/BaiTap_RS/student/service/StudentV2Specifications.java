package com.JavaTraining.BaiTap_RS.student.service;

import java.util.Collection;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class StudentV2Specifications {

    private StudentV2Specifications() {
    }

    /* default */ static Specification<Student> from(ReqFetchStudentV2DTO request, Collection<Long> allowedClassIds) {
        Specification<Student> specification = (root, query, builder) -> builder.conjunction();
        if (StringUtils.hasText(request.getStudentCode())) {
            specification = specification.and((root, query, builder) -> builder.like(
                    root.get("studentCode"), "%" + request.getStudentCode().trim() + "%"));
        }
        if (StringUtils.hasText(request.getStudentName())) {
            specification = specification.and((root, query, builder) -> builder.like(
                    root.get("studentName"), "%" + request.getStudentName().trim() + "%"));
        }
        if (request.getBirthday() != null) {
            specification = specification.and((root, query, builder) -> builder.equal(
                    root.join("studentInfo", JoinType.LEFT).get("dateOfBirth"), request.getBirthday()));
        }
        if (request.getStatus() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), request.getStatus()));
        }
        if (request.getClassId() != null) {
            specification = specification.and(currentClassIn(java.util.List.of(request.getClassId())));
        }
        if (allowedClassIds != null) {
            specification = specification.and(currentClassIn(allowedClassIds));
        }
        return specification;
    }

    private static Specification<Student> currentClassIn(Collection<Long> classIds) {
        return (root, query, builder) -> {
            if (classIds.isEmpty()) {
                return builder.disjunction();
            }
            Subquery<Long> enrollmentQuery = query.subquery(Long.class);
            Root<StudentYearEnrollment> enrollment = enrollmentQuery.from(StudentYearEnrollment.class);
            Root<AcademicYear> academicYear = enrollmentQuery.from(AcademicYear.class);
            enrollmentQuery.select(enrollment.get("studentId"));
            enrollmentQuery.where(
                    builder.equal(enrollment.get("studentId"), root.get("id")),
                    enrollment.get("currentClassId").in(classIds),
                    builder.equal(enrollment.get("status"), EnrollmentStatus.ACTIVE),
                    builder.equal(enrollment.get("academicYearId"), academicYear.get("id")),
                    builder.equal(academicYear.get("status"), AcademicYearStatus.ACTIVE));
            return root.get("id").in(enrollmentQuery);
        };
    }
}
