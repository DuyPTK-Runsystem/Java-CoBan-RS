package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import org.springframework.http.HttpStatus;

final class TranscriptQuerySupport {
    private final StudentAnnualTranscriptRepository annualRepository;
    private final StudentTermTranscriptRepository termRepository;
    private final StudentSubjectTermResultRepository termResultRepository;
    private final StudentSubjectAnnualResultRepository annualResultRepository;
    private final SemesterRepository semesterRepository;
    private final TranscriptAccessGuard accessGuard;
    private final TranscriptTermResponseMapper termMapper;
    private final TranscriptResponseSupport responseSupport;

    public TranscriptQuerySupport(StudentAnnualTranscriptRepository annualRepository,
            StudentTermTranscriptRepository termRepository, StudentSubjectTermResultRepository termResultRepository,
            StudentSubjectAnnualResultRepository annualResultRepository, SemesterRepository semesterRepository,
            TranscriptAccessGuard accessGuard, TranscriptTermResponseMapper termMapper,
            TranscriptResponseSupport responseSupport) {
        this.annualRepository = annualRepository;
        this.termRepository = termRepository;
        this.termResultRepository = termResultRepository;
        this.annualResultRepository = annualResultRepository;
        this.semesterRepository = semesterRepository;
        this.accessGuard = accessGuard;
        this.termMapper = termMapper;
        this.responseSupport = responseSupport;
    }

    public ResStudentTermTranscriptDTO termTranscript(Long studentId, Long semesterId, boolean self) {
        StudentTermTranscript term = termRepository.findByStudentIdAndSemesterId(studentId, semesterId)
                .orElseThrow(() -> notFound("Không tìm thấy bảng điểm học kỳ của học sinh"));
        StudentAnnualTranscript annual = annualRepository.findById(term.getAnnualTranscriptId())
                .filter(value -> value.getStudentId().equals(studentId))
                .orElseThrow(() -> notFound("Không tìm thấy bảng điểm năm học của học sinh"));
        Semester semester = semesterRepository.findById(semesterId)
                .filter(value -> value.getAcademicYearId().equals(annual.getAcademicYearId()))
                .orElseThrow(() -> notFound("Học kỳ không thuộc năm học của bảng điểm"));
        List<StudentSubjectTermResult> results = termResultRepository
                .findAllByTermTranscriptIdOrderBySubjectIdAsc(term.getId());
        Map<Long, com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject> subjects = responseSupport
                .findClassSubjects(results.stream().map(StudentSubjectTermResult::getClassSubjectId).toList());
        if (!self) {
            accessGuard.assertCanRead(studentId, annual.getAcademicYearId(), List.of(semester), subjects.values());
        }
        return new ResStudentTermTranscriptDTO(term.getStudentId(), annual.getAcademicYearId(), term.getSemesterId(),
                term.getCalculationStatus(), term.getSourceVersion(), term.getCalculatedVersion(), term.getCalculatedAt(),
                term.getDtbhk(), responseSupport.findTransferNotes(studentId, annual.getAcademicYearId()),
                termMapper.map(studentId, results, subjects));
    }

    public ResStudentAnnualTranscriptDTO annualTranscript(Long studentId, Long academicYearId, boolean self) {
        StudentAnnualTranscript annual = annualRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .orElseThrow(() -> notFound("Không tìm thấy bảng điểm năm học của học sinh"));
        List<Semester> semesters = semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);
        List<StudentSubjectAnnualResult> annualResults = annualResultRepository
                .findAllByAnnualTranscriptIdOrderBySubjectIdAsc(annual.getId());
        Map<Long, StudentSubjectTermResult> termResults = termResultRepository.findAllById(annualResults.stream()
                .flatMap(result -> java.util.stream.Stream.of(result.getHk1TermResultId(), result.getHk2TermResultId()))
                .filter(java.util.Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(StudentSubjectTermResult::getId, Function.identity()));
        Map<Long, com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject> subjects = responseSupport
                .findClassSubjects(termResults.values().stream().map(StudentSubjectTermResult::getClassSubjectId).toList());
        if (!self) {
            accessGuard.assertCanRead(studentId, academicYearId, semesters, subjects.values());
        }
        return new ResStudentAnnualTranscriptDTO(annual.getStudentId(), annual.getAcademicYearId(),
                annual.getCalculationStatus(), annual.getSourceVersion(), annual.getCalculatedVersion(), annual.getCalculatedAt(),
                annual.getRegularDtbcn(), annual.getFinalDtbcn(), annual.getResultSource(), annual.getLastCalculationTaskId(),
                responseSupport.findTransferNotes(studentId, academicYearId),
                responseSupport.mapAnnualResults(annualResults, termResults));
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }

}
