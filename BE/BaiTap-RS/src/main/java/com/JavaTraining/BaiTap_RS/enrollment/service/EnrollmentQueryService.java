package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResClassStudentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResStudentEnrollmentHistoryDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResTransferHistoryDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResUnassignedStudentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.ClassTransferHistoryRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentQueryService {

    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final ClassTransferHistoryRepository historyRepository;
    private final EnrollmentLookupService lookupService;

    public EnrollmentQueryService(
            StudentYearEnrollmentRepository enrollmentRepository,
            ClassTransferHistoryRepository historyRepository,
            EnrollmentLookupService lookupService) {
        this.enrollmentRepository = enrollmentRepository;
        this.historyRepository = historyRepository;
        this.lookupService = lookupService;
    }

    // FR-ENROLL-004: only active students without any year enrollment are candidates.
    @Transactional(readOnly = true)
    public List<ResUnassignedStudentDTO> listUnassignedStudents(Long academicYearId) {
        lookupService.findAcademicYear(academicYearId);
        return enrollmentRepository.findUnassignedStudents(academicYearId)
                .stream()
                .map(student -> new ResUnassignedStudentDTO(
                        student.getId(), student.getStudentCode(), student.getStudentName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResClassStudentDTO> listClassStudents(Long classId) {
        lookupService.findSchoolClass(classId);
        return enrollmentRepository.findByCurrentClassIdAndStatusOrderByStudentIdAsc(
                        classId,
                        EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toClassStudent)
                .toList();
    }

    // FR-ENROLL-005 and BR-ENROLL-002: return the append-only class history.
    @Transactional(readOnly = true)
    public List<ResStudentEnrollmentHistoryDTO> listStudentHistory(Long studentId) {
        Student student = lookupService.findStudent(studentId);
        return enrollmentRepository.findByStudentIdOrderByEnrolledAtAsc(studentId)
                .stream()
                .map(enrollment -> toStudentHistoryResponse(enrollment, student))
                .toList();
    }

    private ResClassStudentDTO toClassStudent(StudentYearEnrollment enrollment) {
        Student student = lookupService.findStudent(enrollment.getStudentId());
        return new ResClassStudentDTO(
                student.getId(),
                student.getStudentCode(),
                student.getStudentName(),
                enrollment.getId());
    }

    private ResStudentEnrollmentHistoryDTO toStudentHistoryResponse(
            StudentYearEnrollment enrollment,
            Student student) {
        SchoolClass currentClass = lookupService.findSchoolClass(enrollment.getCurrentClassId());
        ResEnrollmentDTO enrollmentResponse = toEnrollmentResponse(enrollment, student, currentClass);
        List<ResTransferHistoryDTO> transfers = historyRepository
                .findByEnrollmentIdOrderByEffectiveAtAsc(enrollment.getId())
                .stream()
                .map(history -> new ResTransferHistoryDTO(
                        history.getId(),
                        history.getFromClassId(),
                        history.getToClassId(),
                        history.getEffectiveAt(),
                        history.getReason(),
                        history.getApprovedBy()))
                .toList();
        return new ResStudentEnrollmentHistoryDTO(enrollmentResponse, transfers);
    }

    private ResEnrollmentDTO toEnrollmentResponse(
            StudentYearEnrollment enrollment,
            Student student,
            SchoolClass schoolClass) {
        return new ResEnrollmentDTO(
                enrollment.getId(),
                student.getId(),
                student.getStudentCode(),
                student.getStudentName(),
                enrollment.getAcademicYearId(),
                schoolClass.getId(),
                schoolClass.getClassCode(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt());
    }
}
