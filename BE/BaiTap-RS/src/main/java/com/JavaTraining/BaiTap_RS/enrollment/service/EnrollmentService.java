package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqBulkCreateEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqCreateEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResEnrollmentMutationDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.ClassTransferHistory;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.ClassTransferHistoryRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final ClassTransferHistoryRepository historyRepository;
    private final EnrollmentLookupService lookupService;
    private final EnrollmentCapacityService capacityService;
    private final EnrollmentAuditService auditService;

    public EnrollmentService(
            StudentYearEnrollmentRepository enrollmentRepository,
            ClassTransferHistoryRepository historyRepository,
            EnrollmentLookupService lookupService,
            EnrollmentCapacityService capacityService,
            EnrollmentAuditService auditService) {
        this.enrollmentRepository = enrollmentRepository;
        this.historyRepository = historyRepository;
        this.lookupService = lookupService;
        this.capacityService = capacityService;
        this.auditService = auditService;
    }

    // FR-ENROLL-001 and BR-ENROLL-001: create is rejected when the year already has an enrollment.
    @Transactional
    public ResEnrollmentMutationDTO createEnrollment(ReqCreateEnrollmentDTO request) {
        AcademicYear year = lookupService.findAcademicYear(request.academicYearId());
        SchoolClass schoolClass = validateTargetClass(request.classId(), year);
        Student student = lookupService.findStudent(request.studentId());
        validateNewEnrollment(student, year);
        LocalDateTime enrolledAt = defaultTime(request.enrolledAt());
        StudentYearEnrollment enrollment = persistEnrollment(student, year, schoolClass, enrolledAt);
        ResEnrollmentDTO response = toEnrollmentResponse(enrollment, student, schoolClass);
        return new ResEnrollmentMutationDTO(
                List.of(response),
                capacityService.capacityWarnings(List.of(schoolClass)));
    }

    // FR-ENROLL-002 and BR-COMMON-003: validate every item before persisting any item.
    @Transactional
    public ResEnrollmentMutationDTO createBulkEnrollment(ReqBulkCreateEnrollmentDTO request) {
        AcademicYear year = lookupService.findAcademicYear(request.academicYearId());
        SchoolClass schoolClass = validateTargetClass(request.classId(), year);
        Set<Long> uniqueStudentIds = new HashSet<>(request.studentIds());
        if (uniqueStudentIds.size() != request.studentIds().size()) {
            throw new AppException(HttpStatus.CONFLICT, "Danh sách học sinh bị trùng");
        }
        List<Student> students = lookupService.findStudents(uniqueStudentIds);
        students.forEach(student -> validateNewEnrollment(student, year));
        Map<Long, Student> studentsById = students.stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        LocalDateTime enrolledAt = defaultTime(request.enrolledAt());
        List<ResEnrollmentDTO> responses = request.studentIds()
                .stream()
                .map(studentId -> {
                    Student student = studentsById.get(studentId);
                    StudentYearEnrollment enrollment = persistEnrollment(student, year, schoolClass, enrolledAt);
                    return toEnrollmentResponse(enrollment, student, schoolClass);
                })
                .toList();
        return new ResEnrollmentMutationDTO(responses, capacityService.capacityWarnings(List.of(schoolClass)));
    }

    // FR-ENROLL-003, BR-ENROLL-002 and NFR-RELIABILITY-005: update and history/audit share one transaction.
    @Transactional
    public ResEnrollmentMutationDTO transferEnrollment(Long enrollmentId, ReqTransferEnrollmentDTO request) {
        StudentYearEnrollment enrollment = lookupService.findEnrollment(enrollmentId);
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ enrollment ACTIVE mới được chuyển lớp");
        }
        validateEffectiveAt(enrollmentId, request.effectiveAt());
        AcademicYear year = lookupService.findAcademicYear(enrollment.getAcademicYearId());
        SchoolClass sourceClass = lookupService.findSchoolClass(enrollment.getCurrentClassId());
        SchoolClass targetClass = validateTargetClass(request.targetClassId(), year);
        if (sourceClass.getId().equals(targetClass.getId())) {
            throw new AppException(HttpStatus.CONFLICT, "Lớp đích phải khác lớp hiện tại");
        }
        Long actorUserId = AuditContext.currentUserId();
        enrollment.setCurrentClassId(targetClass.getId());
        ClassTransferHistory history = historyRepository.save(new ClassTransferHistory(
                enrollment.getId(),
                sourceClass.getId(),
                targetClass.getId(),
                request.effectiveAt(),
                request.reason(),
                actorUserId));
        auditService.writeTransferAudit(actorUserId, enrollment, sourceClass, targetClass, history);
        Student student = lookupService.findStudent(enrollment.getStudentId());
        ResEnrollmentDTO response = toEnrollmentResponse(enrollment, student, targetClass);
        return new ResEnrollmentMutationDTO(
                List.of(response),
                capacityService.capacityWarnings(List.of(sourceClass, targetClass)));
    }

    // BR-ENROLL-003/004: an applied transfer cannot be scheduled in the future or move history backward.
    private void validateEffectiveAt(Long enrollmentId, LocalDateTime effectiveAt) {
        LocalDateTime now = LocalDateTime.now();
        if (effectiveAt.isAfter(now)) {
            throw new AppException(HttpStatus.CONFLICT, "Ngày hiệu lực không được ở tương lai");
        }
        historyRepository.findTopByEnrollmentIdOrderByEffectiveAtDesc(enrollmentId)
                .map(ClassTransferHistory::getEffectiveAt)
                .filter(effectiveAt::isBefore)
                .ifPresent(latestEffectiveAt -> {
                    throw new AppException(
                            HttpStatus.CONFLICT,
                            "Ngày hiệu lực không được trước history gần nhất: " + latestEffectiveAt);
                });
    }

    private StudentYearEnrollment persistEnrollment(
            Student student,
            AcademicYear year,
            SchoolClass schoolClass,
            LocalDateTime enrolledAt) {
        StudentYearEnrollment enrollment = new StudentYearEnrollment(
                student.getId(),
                year.getId(),
                schoolClass.getId(),
                EnrollmentStatus.ACTIVE,
                enrolledAt);
        enrollmentRepository.save(enrollment);
        historyRepository.save(new ClassTransferHistory(
                enrollment.getId(),
                null,
                schoolClass.getId(),
                enrolledAt,
                null,
                AuditContext.currentUserId()));
        return enrollment;
    }

    private SchoolClass validateTargetClass(Long classId, AcademicYear year) {
        SchoolClass schoolClass = lookupService.findSchoolClass(classId);
        if (!year.getId().equals(schoolClass.getAcademicYearId())) {
            throw new AppException(HttpStatus.CONFLICT, "Lớp không thuộc năm học đã chọn");
        }
        if (year.getStatus() != AcademicYearStatus.ACTIVE) {
            throw new AppException(HttpStatus.CONFLICT, "Năm học chưa ACTIVE");
        }
        if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
            throw new AppException(HttpStatus.CONFLICT, "Lớp chưa ACTIVE");
        }
        return schoolClass;
    }

    private void validateNewEnrollment(Student student, AcademicYear year) {
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new AppException(HttpStatus.CONFLICT, "Học sinh không còn ACTIVE");
        }
        if (enrollmentRepository.existsByStudentIdAndAcademicYearId(student.getId(), year.getId())) {
            throw new AppException(HttpStatus.CONFLICT, "Học sinh đã có enrollment trong năm học");
        }
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

    private LocalDateTime defaultTime(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }
}
