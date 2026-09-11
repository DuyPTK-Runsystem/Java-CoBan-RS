package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class ClassTranscriptScopeReader {

    private final SchoolClassRepository schoolClassRepository;
    private final SemesterRepository semesterRepository;
    private final TranscriptAccessGuard accessGuard;

    /* default */ Scope forTerm(Long classId, Long semesterId) {
        SchoolClass schoolClass = findClass(classId);
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> notFound("Không tìm thấy học kỳ"));
        if (!semester.getAcademicYearId().equals(schoolClass.getAcademicYearId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Học kỳ không thuộc năm học của lớp");
        }
        accessGuard.assertCanReadClass(classId, schoolClass.getAcademicYearId(), List.of(semester));
        return new Scope(schoolClass, List.of(semester));
    }

    /* default */ Scope forAnnual(Long classId, Long academicYearId) {
        SchoolClass schoolClass = findClass(classId);
        if (!schoolClass.getAcademicYearId().equals(academicYearId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Năm học không khớp với lớp");
        }
        List<Semester> semesters = semesterRepository
                .findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);
        accessGuard.assertCanReadClass(classId, academicYearId, semesters);
        return new Scope(schoolClass, semesters);
    }

    private SchoolClass findClass(Long classId) {
        return schoolClassRepository.findById(classId)
                .orElseThrow(() -> notFound("Không tìm thấy lớp học"));
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }

    /* default */ record Scope(SchoolClass schoolClass, List<Semester> semesters) {
    }
}
