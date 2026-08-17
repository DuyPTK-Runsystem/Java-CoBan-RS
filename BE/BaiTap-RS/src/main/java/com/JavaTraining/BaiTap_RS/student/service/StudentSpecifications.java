package com.JavaTraining.BaiTap_RS.student.service;

import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqFetchStudentDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class StudentSpecifications {

    private StudentSpecifications() {
    }

    /* default */ static Specification<Student> from(ReqFetchStudentDTO request) {
        Specification<Student> specification = empty();
        if (StringUtils.hasText(request.getStudentCode())) {
            specification = specification.and((root, query, builder) ->
                    builder.like(
                            root.get(StudentSortResolver.STUDENT_CODE_FIELD),
                            "%" + request.getStudentCode().trim() + "%"));
        }
        if (StringUtils.hasText(request.getStudentName())) {
            specification = specification.and((root, query, builder) ->
                    builder.like(
                            root.get(StudentSortResolver.STUDENT_NAME_FIELD),
                            "%" + request.getStudentName().trim() + "%"));
        }
        if (request.getBirthday() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.join(StudentSortResolver.STUDENT_INFO_FIELD, JoinType.LEFT)
                                    .get(StudentSortResolver.DATE_OF_BIRTH_FIELD),
                            request.getBirthday()));
        }
        return specification;
    }

    private static Specification<Student> empty() {
        return (root, query, builder) -> builder.conjunction();
    }
}
