package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


/**
 * Tra cứu danh sách học sinh (roster) dùng cho score grid.
 * Sử dụng enrollment repository và student repository có sẵn.
 */
@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class EnrollmentRosterService {

    private final EnrollmentRosterRepository rosterRepository;
    private final StudentRepository studentRepository;

    public EnrollmentRosterService(
            EnrollmentRosterRepository rosterRepository,
            StudentRepository studentRepository) {
        this.rosterRepository = rosterRepository;
        this.studentRepository = studentRepository;
    }

    public Page<StudentYearEnrollment> findActiveRoster(
            Long classId, Long semesterId, Pageable pageable) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        EnrollmentRosterService.class,
                        "EnrollmentRosterService.findActiveRoster");
        return rosterRepository.findActiveByClassId(classId, pageable);
    }

    public Map<Long, Student> loadStudents(List<Long> studentIds) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                EnrollmentRosterService.class,
                "EnrollmentRosterService.loadStudents");
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
    }
}
