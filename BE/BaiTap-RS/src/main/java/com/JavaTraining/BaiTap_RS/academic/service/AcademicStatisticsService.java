package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResAcademicYearStatisticsDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResClassStatisticDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResGradeStatisticDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResCapacityWarningDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.enrollment.service.EnrollmentCapacityService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class AcademicStatisticsService {

        private final AcademicYearRepository academicYearRepository;
        private final GradeLevelRepository gradeLevelRepository;
        private final SchoolClassRepository schoolClassRepository;
        private final StudentYearEnrollmentRepository enrollmentRepository;
        private final EnrollmentCapacityService enrollmentCapacityService;

        public AcademicStatisticsService(
                        AcademicYearRepository academicYearRepository,
                        GradeLevelRepository gradeLevelRepository,
                        SchoolClassRepository schoolClassRepository,
                        StudentYearEnrollmentRepository enrollmentRepository,
                        EnrollmentCapacityService enrollmentCapacityService) {
                this.academicYearRepository = academicYearRepository;
                this.gradeLevelRepository = gradeLevelRepository;
                this.schoolClassRepository = schoolClassRepository;
                this.enrollmentRepository = enrollmentRepository;
                this.enrollmentCapacityService = enrollmentCapacityService;
        }

        @Transactional(readOnly = true)
        public ResAcademicYearStatisticsDTO getAcademicYearStatistics(Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                                AcademicStatisticsService.class,
                                "AcademicStatisticsService.getAcademicYearStatistics");

                AcademicYear year = academicYearRepository.findById(academicYearId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học"));

                List<SchoolClass> classes = schoolClassRepository
                                .findAllByAcademicYearIdOrderByClassCodeAsc(year.getId());

                List<StudentYearEnrollment> activeEnrollments = enrollmentRepository
                                .findByAcademicYearIdAndStatusOrderByStudentIdAsc(year.getId(),
                                                EnrollmentStatus.ACTIVE);

                Map<Long, Long> classStudentCounts = activeEnrollments.stream()
                                .filter(e -> e.getCurrentClassId() != null)
                                .collect(Collectors.groupingBy(StudentYearEnrollment::getCurrentClassId,
                                                Collectors.counting()));

                List<ResCapacityWarningDTO> warnings = enrollmentCapacityService.capacityWarnings(classes);
                Map<Long, ResCapacityWarningDTO> warningByClassId = warnings.stream()
                                .collect(Collectors.toMap(ResCapacityWarningDTO::classId, Function.identity(),
                                                (a, b) -> a));

                List<GradeLevel> gradeLevels = gradeLevelRepository
                                .findAll(Sort.by(Sort.Direction.ASC, "displayOrder"));

                List<ResGradeStatisticDTO> gradeStatistics = new ArrayList<>();
                Map<Long, Double> gradeAverageMap = new HashMap<>();

                for (GradeLevel grade : gradeLevels) {
                        List<SchoolClass> activeClassesInGrade = classes.stream()
                                        .filter(c -> grade.getId().equals(c.getGradeLevelId())
                                                        && c.getStatus() == SchoolClassStatus.ACTIVE)
                                        .toList();

                        long activeClassCount = activeClassesInGrade.size();
                        long activeStudentCount = activeClassesInGrade.stream()
                                        .mapToLong(c -> classStudentCounts.getOrDefault(c.getId(), 0L))
                                        .sum();

                        double average = activeClassCount == 0 ? 0.0 : (double) activeStudentCount / activeClassCount;
                        gradeAverageMap.put(grade.getId(), average);

                        gradeStatistics.add(new ResGradeStatisticDTO(
                                        grade.getId(),
                                        activeClassCount,
                                        activeStudentCount));
                }

                List<ResClassStatisticDTO> classStatistics = classes.stream()
                                .map(schoolClass -> {
                                        long studentCount = classStudentCounts.getOrDefault(schoolClass.getId(), 0L);
                                        Double gradeAverage = gradeAverageMap.get(schoolClass.getGradeLevelId());
                                        ResCapacityWarningDTO warning = warningByClassId.get(schoolClass.getId());

                                        return new ResClassStatisticDTO(
                                                        schoolClass.getId(),
                                                        schoolClass.getClassCode(),
                                                        schoolClass.getClassName(),
                                                        schoolClass.getGradeLevelId(),
                                                        schoolClass.getCapacity(),
                                                        studentCount,
                                                        gradeAverage,
                                                        warning);
                                })
                                .toList();

                return new ResAcademicYearStatisticsDTO(
                                year.getId(),
                                gradeStatistics,
                                classStatistics,
                                warnings.size());
        }
}
