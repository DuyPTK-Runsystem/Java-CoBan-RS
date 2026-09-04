package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import org.springframework.stereotype.Component;

@Component
public class DemoAcademicCatalogSeeder {

    private static final String ACADEMIC_YEAR_CODE = "2026-2027";
    private static final LocalDate YEAR_START = LocalDate.of(2026, 9, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2027, 5, 31);
    private static final LocalDate HK1_START = LocalDate.of(2026, 9, 1);
    private static final LocalDate HK1_END = LocalDate.of(2026, 12, 31);
    private static final LocalDate HK2_START = LocalDate.of(2027, 1, 1);
    private static final LocalDate HK2_END = LocalDate.of(2027, 5, 31);

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;

    public DemoAcademicCatalogSeeder(
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository) {
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
    }

    public AcademicYear seedAcademicYear() {
        return academicYearRepository.findAll().stream()
                .filter(year -> ACADEMIC_YEAR_CODE.equals(year.getCode()))
                .findFirst()
                .orElseGet(() -> academicYearRepository.save(new AcademicYear(
                        ACADEMIC_YEAR_CODE,
                        YEAR_START,
                        YEAR_END,
                        AcademicYearStatus.ACTIVE,
                        "Demo fixture Plan 048")));
    }

    public List<Semester> seedSemesters(AcademicYear academicYear) {
        return List.of(
                ensureSemester(academicYear, "HK1", "HK1 2026 - 2027", 1, HK1_START, HK1_END,
                        SemesterStatus.ACTIVE),
                ensureSemester(academicYear, "HK2", "HK2 2026 - 2027", 2, HK2_START, HK2_END,
                        SemesterStatus.DRAFT));
    }

    public Map<Integer, GradeLevel> seedGrades() {
        Map<Integer, GradeLevel> grades = new LinkedHashMap<>();
        List<GradeLevel> existingGrades = new ArrayList<>(gradeLevelRepository.findAll());
        for (int level = 6; level <= 9; level++) {
            final int currentLevel = level;
            GradeLevel grade = existingGrades.stream()
                    .filter(existing -> currentLevel == existing.getLevel())
                    .findFirst()
                    .orElseGet(() -> createGrade(existingGrades, currentLevel));
            grades.put(level, grade);
        }
        for (int level = 6; level < 9; level++) {
            GradeLevel current = grades.get(level);
            current.setNextGradeId(grades.get(level + 1).getId());
            gradeLevelRepository.save(current);
        }
        return grades;
    }

    public List<SchoolClass> seedClasses(
            AcademicYear academicYear,
            Map<Integer, GradeLevel> grades) {
        List<SchoolClass> classes = new ArrayList<>(
                schoolClassRepository.findAllByAcademicYearIdOrderByClassCodeAsc(academicYear.getId()));
        for (int level = 6; level <= 9; level++) {
            final int currentLevel = level;
            for (int section = 1; section <= 2; section++) {
                String code = currentLevel + "A" + section;
                if (classes.stream().noneMatch(existing -> code.equals(existing.getClassCode()))) {
                    classes.add(schoolClassRepository.save(new SchoolClass(
                            academicYear.getId(),
                            grades.get(currentLevel).getId(),
                            code,
                            "Lớp " + code,
                            40,
                            SchoolClassStatus.ACTIVE)));
                }
            }
        }
        classes.sort(Comparator.comparing(SchoolClass::getClassCode));
        return classes;
    }

    public Map<String, Subject> seedSubjects() {
        List<SubjectSeed> seeds = List.of(
                new SubjectSeed("TOAN", "Toán", SubjectType.ACADEMIC),
                new SubjectSeed("VAT_LY", "Vật lí", SubjectType.ACADEMIC),
                new SubjectSeed("HOA_HOC", "Hóa học", SubjectType.ACADEMIC),
                new SubjectSeed("SINH_HOC", "Sinh học", SubjectType.ACADEMIC),
                new SubjectSeed("NGU_VAN", "Ngữ Văn", SubjectType.ACADEMIC),
                new SubjectSeed("NGOAI_NGU", "Ngoại ngữ", SubjectType.ACADEMIC),
                new SubjectSeed("LICH_SU", "Lịch sử", SubjectType.ACADEMIC),
                new SubjectSeed("DIA_LY", "Địa lí", SubjectType.ACADEMIC),
                new SubjectSeed("GDCD", "Giáo dục công dân", SubjectType.ACADEMIC),
                new SubjectSeed("TIN_HOC", "Tin học", SubjectType.ACADEMIC),
                new SubjectSeed("CONG_NGHE", "Công nghệ", SubjectType.ACADEMIC),
                new SubjectSeed("NGHE_DIEN", "Nghề phổ thông - Điện dân dụng", SubjectType.SKILL),
                new SubjectSeed("NGHE_NONG_NGHIEP", "Nghề phổ thông - Nông nghiệp", SubjectType.SKILL));
        Map<String, Subject> subjects = new LinkedHashMap<>();
        List<Subject> existingSubjects = new ArrayList<>(subjectRepository.findAll());
        for (SubjectSeed seed : seeds) {
            Subject subject = existingSubjects.stream()
                    .filter(existing -> seed.code().equals(existing.getCode()))
                    .findFirst()
                    .orElseGet(() -> createSubject(existingSubjects, seed));
            subjects.put(seed.code(), subject);
        }
        return subjects;
    }

    private Semester ensureSemester(
            AcademicYear academicYear,
            String code,
            String name,
            int displayOrder,
            LocalDate startDate,
            LocalDate endDate,
            SemesterStatus status) {
        return semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYear.getId()).stream()
                .filter(semester -> code.equals(semester.getCode()))
                .findFirst()
                .orElseGet(() -> semesterRepository.save(new Semester(
                        academicYear.getId(), code, name, displayOrder, startDate, endDate, null, status)));
    }

    private GradeLevel createGrade(List<GradeLevel> existingGrades, int level) {
        GradeLevel grade = gradeLevelRepository.save(new GradeLevel(
                "KHOI_" + level,
                "Khối " + level,
                level,
                level,
                null,
                true,
                "Demo fixture Plan 048"));
        existingGrades.add(grade);
        return grade;
    }

    private Subject createSubject(List<Subject> existingSubjects, SubjectSeed seed) {
        Subject subject = subjectRepository.save(new Subject(
                seed.code(),
                seed.name(),
                seed.type(),
                ApplicationScope.GRADE,
                SubjectStatus.ACTIVE));
        existingSubjects.add(subject);
        return subject;
    }

    private record SubjectSeed(String code, String name, SubjectType type) {
    }
}
