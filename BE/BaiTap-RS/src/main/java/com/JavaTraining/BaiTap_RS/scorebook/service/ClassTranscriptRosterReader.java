package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class ClassTranscriptRosterReader {

    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    /* default */ Roster read(Long classId) {
        List<Long> studentIds = enrollmentRepository
                .findByCurrentClassIdAndStatusOrderByStudentIdAsc(classId, EnrollmentStatus.ACTIVE).stream()
                .map(StudentYearEnrollment::getStudentId).toList();
        Map<Long, StudentSummary> students = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId,
                        student -> new StudentSummary(student.getStudentCode(), student.getStudentName())));
        return new Roster(studentIds, students);
    }

    /* default */ record Roster(List<Long> studentIds, Map<Long, StudentSummary> students) {
    }

    /* default */ record StudentSummary(String studentCode, String fullName) {
    }
}
