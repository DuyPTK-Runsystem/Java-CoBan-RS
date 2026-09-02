package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResTranscriptCalculationStatusDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;

final class TranscriptStatusSupport {
    private final StudentAnnualTranscriptRepository annualRepository;
    private final StudentTermTranscriptRepository termRepository;
    private final SemesterRepository semesterRepository;
    private final TranscriptAccessGuard accessGuard;
    private final StudentRepository studentRepository;

    public TranscriptStatusSupport(StudentAnnualTranscriptRepository annualRepository,
            StudentTermTranscriptRepository termRepository, SemesterRepository semesterRepository,
            TranscriptAccessGuard accessGuard, StudentRepository studentRepository) {
        this.annualRepository = annualRepository;
        this.termRepository = termRepository;
        this.semesterRepository = semesterRepository;
        this.accessGuard = accessGuard;
        this.studentRepository = studentRepository;
    }

    public ResTranscriptCalculationStatusDTO termStatus(Long studentId, Long semesterId, boolean self) {
        StudentTermTranscript term = termRepository.findByStudentIdAndSemesterId(studentId, semesterId)
                .orElseThrow(() -> notFound("Không tìm thấy trạng thái bảng điểm học kỳ của học sinh"));
        StudentAnnualTranscript annual = annualRepository.findById(term.getAnnualTranscriptId())
                .filter(value -> value.getStudentId().equals(studentId))
                .orElseThrow(() -> notFound("Không tìm thấy bảng điểm năm học của học sinh"));
        Semester semester = semesterRepository.findById(semesterId)
                .filter(value -> value.getAcademicYearId().equals(annual.getAcademicYearId()))
                .orElseThrow(() -> notFound("Học kỳ không thuộc năm học của bảng điểm"));
        if (!self) {
            accessGuard.assertCanRead(studentId, annual.getAcademicYearId(), List.of(semester), List.of());
        }
        return status(studentId, semester.getAcademicYearId(), semesterId, term.getCalculationStatus(),
                term.getSourceVersion(), term.getCalculatedVersion(), term.getCalculatedAt(), term.getLastError());
    }

    public ResTranscriptCalculationStatusDTO annualStatus(Long studentId, Long academicYearId, boolean self) {
        StudentAnnualTranscript annual = annualRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .orElseThrow(() -> notFound("Không tìm thấy trạng thái bảng điểm năm học của học sinh"));
        List<Semester> semesters = semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);
        if (!self) {
            accessGuard.assertCanRead(studentId, academicYearId, semesters, List.of());
        }
        return status(studentId, academicYearId, null, annual.getCalculationStatus(), annual.getSourceVersion(),
                annual.getCalculatedVersion(), annual.getCalculatedAt(), annual.getLastError());
    }

    private ResTranscriptCalculationStatusDTO status(Long studentId, Long yearId, Long semesterId, CalculationStatus state,
            Long sourceVersion, Long calculatedVersion, LocalDateTime calculatedAt, String lastError) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> notFound("Không tìm thấy học sinh"));
        boolean upToDate = state == CalculationStatus.FINISH && sourceVersion != null && sourceVersion.equals(calculatedVersion);
        return new ResTranscriptCalculationStatusDTO(studentId, student.getStudentCode(), yearId, semesterId, state,
                sourceVersion, calculatedVersion, upToDate, calculatedAt, lastError);
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }
}
