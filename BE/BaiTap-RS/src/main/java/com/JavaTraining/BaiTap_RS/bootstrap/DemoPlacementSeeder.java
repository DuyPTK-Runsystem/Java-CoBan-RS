package com.JavaTraining.BaiTap_RS.bootstrap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSessionStatus;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementCandidateRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementResultRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementSessionRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.seed.demo.enabled", havingValue = "true")
public class DemoPlacementSeeder implements ApplicationRunner {

    private static final String ACADEMIC_YEAR_CODE = "2026-2027";
    private static final String RULE_VERSION = "placement-rule-v1";
    private static final String[] SESSION_KEYS = {
        "PLACE-081-DRAFT", "PLACE-081-SIM", "PLACE-081-READY", "PLACE-081-CONFIRMED",
        "PLACE-081-CANCELLED"
    };

    private final AcademicYearRepository academicYearRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final PlacementSessionRepository sessionRepository;
    private final DemoPlacementSeedWriter seedWriter;

    public DemoPlacementSeeder(
            AcademicYearRepository academicYearRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository,
            PlacementSessionRepository sessionRepository,
            PlacementCandidateRepository candidateRepository,
            PlacementResultRepository resultRepository) {
        this.academicYearRepository = academicYearRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.seedWriter = new DemoPlacementSeedWriter(candidateRepository, resultRepository);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AcademicYear academicYear = academicYearRepository.findAll().stream()
                .filter(year -> ACADEMIC_YEAR_CODE.equals(year.getCode()))
                .findFirst()
                .orElse(null);
        GradeLevel targetGrade = gradeLevelRepository.findAll().stream()
                .filter(grade -> Integer.valueOf(7).equals(grade.getLevel()))
                .findFirst()
                .orElse(null);
        if (academicYear == null || targetGrade == null) {
            return;
        }

        List<SchoolClass> targetClasses = schoolClassRepository
                .findAllByAcademicYearIdOrderByClassCodeAsc(academicYear.getId()).stream()
                .filter(schoolClass -> targetGrade.getId().equals(schoolClass.getGradeLevelId()))
                .filter(schoolClass -> Set.of("7A1", "7A2").contains(schoolClass.getClassCode()))
                .toList();
        if (targetClasses.size() < 2) {
            return;
        }

        List<String> studentCodes = seedWriter.studentCodes();
        Map<String, Student> students = studentRepository.findAllByStudentCodeIn(studentCodes).stream()
                .collect(Collectors.toMap(Student::getStudentCode, Function.identity()));
        if (students.size() != studentCodes.size()) {
            return;
        }

        for (int index = 0; index < SESSION_KEYS.length; index++) {
            String key = SESSION_KEYS[index];
            PlacementSession session = findSession(key);
            if (session == null) {
                session = sessionRepository.save(createSession(
                        academicYear.getId(), targetGrade.getId(), key, targetClasses));
                session.setStatus(statusAt(index));
                if (statusAt(index) == PlacementSessionStatus.CONFIRMED) {
                    session.setConfirmIdempotencyKey(key);
                }
                session = sessionRepository.save(session);
            }
            seedWriter.seedRows(session, targetGrade, targetClasses, students);
        }
    }

    private PlacementSession findSession(String key) {
        return sessionRepository.findAll().stream()
                .filter(session -> session.getScopeSnapshot() != null
                        && session.getScopeSnapshot().contains(key))
                .findFirst()
                .orElse(null);
    }

    private PlacementSession createSession(
            Long academicYearId,
            Long targetGradeId,
            String key,
            List<SchoolClass> targetClasses) {
        return new PlacementSession(
                academicYearId, targetGradeId, RULE_VERSION, scopeSnapshot(key, targetClasses), null);
    }

    private PlacementSessionStatus statusAt(int index) {
        return switch (index) {
            case 0 -> PlacementSessionStatus.DRAFT;
            case 1 -> PlacementSessionStatus.SIMULATED;
            case 2 -> PlacementSessionStatus.READY_FOR_CONFIRM;
            case 3 -> PlacementSessionStatus.CONFIRMED;
            default -> PlacementSessionStatus.CANCELLED;
        };
    }

    private String scopeSnapshot(String key, List<SchoolClass> targetClasses) {
        return "{\"seedKey\":\"" + key + "\",\"source\":\"Plan 081\",\"ruleVersion\":\""
                + RULE_VERSION + "\",\"targetClassCodes\":[\"" + targetClasses.get(0).getClassCode()
                + "\",\"" + targetClasses.get(1).getClassCode() + "\"]}";
    }

}
