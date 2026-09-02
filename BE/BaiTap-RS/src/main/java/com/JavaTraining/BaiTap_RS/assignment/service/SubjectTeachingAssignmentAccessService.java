package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class SubjectTeachingAssignmentAccessService {

    private final SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;

    public SubjectTeachingAssignmentAccessService(
            SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository) {
        this.subjectTeachingAssignmentRepository = subjectTeachingAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAssignment(Long teacherId, Long classSubjectId, LocalDate effectiveDate) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectTeachingAssignmentAccessService.class,
                "SubjectTeachingAssignmentAccessService.hasActiveAssignment");
        return subjectTeachingAssignmentRepository.hasActiveAssignment(teacherId, classSubjectId, effectiveDate);
    }

    @Transactional(readOnly = true)
    public void assertActiveAssignment(Long teacherId, Long classSubjectId, LocalDate effectiveDate) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectTeachingAssignmentAccessService.class,
                "SubjectTeachingAssignmentAccessService.assertActiveAssignment");
        if (!hasActiveAssignment(teacherId, classSubjectId, effectiveDate)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Giáo viên không có phân công GVBM ACTIVE");
        }
    }
}
