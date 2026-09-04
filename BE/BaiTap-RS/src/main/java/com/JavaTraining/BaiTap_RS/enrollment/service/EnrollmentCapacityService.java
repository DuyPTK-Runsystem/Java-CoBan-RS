package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResCapacityWarningDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class EnrollmentCapacityService {

    private static final double LOWER_BALANCE_THRESHOLD = 0.8;
    private static final double UPPER_BALANCE_THRESHOLD = 1.2;

    private final SchoolClassRepository schoolClassRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final EnrollmentLookupService lookupService;

    public EnrollmentCapacityService(
            SchoolClassRepository schoolClassRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            EnrollmentLookupService lookupService) {
        this.schoolClassRepository = schoolClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lookupService = lookupService;
    }

    public List<ResCapacityWarningDTO> capacityWarnings(List<SchoolClass> classes) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                EnrollmentCapacityService.class,
                "EnrollmentCapacityService.capacityWarnings");
        return new ArrayList<>(classes.stream()
                .map(this::capacityWarning)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(
                        ResCapacityWarningDTO::classId,
                        Function.identity(),
                        (first, duplicate) -> first))
                .values());
    }

    private Optional<ResCapacityWarningDTO> capacityWarning(SchoolClass schoolClass) {
        GradeLevel grade = lookupService.findGradeForClass(schoolClass);
        long gradeClassCount = countActiveClasses(schoolClass, grade);
        if (gradeClassCount == 0) {
            return Optional.empty();
        }
        return buildWarning(schoolClass, grade, gradeClassCount);
    }

    private long countActiveClasses(SchoolClass schoolClass, GradeLevel grade) {
        return schoolClassRepository.countByAcademicYearIdAndGradeLevelIdAndStatus(
                schoolClass.getAcademicYearId(),
                grade.getId(),
                SchoolClassStatus.ACTIVE);
    }

    private Optional<ResCapacityWarningDTO> buildWarning(
            SchoolClass schoolClass,
            GradeLevel grade,
            long gradeClassCount) {
        long classCount = enrollmentRepository.countByCurrentClassIdAndStatus(
                schoolClass.getId(),
                EnrollmentStatus.ACTIVE);
        long gradeStudentCount = enrollmentRepository.countByAcademicYearAndGradeAndStatus(
                schoolClass.getAcademicYearId(),
                grade.getId(),
                EnrollmentStatus.ACTIVE);
        double average = (double) gradeStudentCount / gradeClassCount;
        if (average <= 0 || balanced(classCount, average)) {
            return Optional.empty();
        }
        return Optional.of(new ResCapacityWarningDTO(
                schoolClass.getId(),
                schoolClass.getAcademicYearId(),
                grade.getId(),
                classCount,
                average,
                "Sĩ số lớp lệch quá 20% so với trung bình khối"));
    }

    private boolean balanced(long classCount, double average) {
        return classCount >= average * LOWER_BALANCE_THRESHOLD
                && classCount <= average * UPPER_BALANCE_THRESHOLD;
    }
}
