package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResTranscriptCalculationStatusDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class TranscriptQueryService {

    private final StudentAnnualTranscriptRepository annualTranscriptRepository;
    private final StudentTermTranscriptRepository termTranscriptRepository;
    private final StudentSubjectTermResultRepository termResultRepository;
    private final StudentSubjectAnnualResultRepository annualResultRepository;
    private final SemesterRepository semesterRepository;
    private final TranscriptAccessGuard accessGuard;
    private final TranscriptTermResponseMapper termResponseMapper;
    private final TranscriptResponseSupport responseSupport;
    private final TranscriptCurrentStudentResolver currentStudentResolver;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public ResStudentTermTranscriptDTO getMyTermTranscript(Long semesterId) {
        return getTermTranscript(currentStudentResolver.currentStudentId(), semesterId, true);
    }

    @Transactional(readOnly = true)
    public ResStudentTermTranscriptDTO getTermTranscript(Long studentId, Long semesterId) {
        return getTermTranscript(studentId, semesterId, false);
    }

    @Transactional(readOnly = true)
    public ResStudentAnnualTranscriptDTO getMyAnnualTranscript(Long academicYearId) {
        return getAnnualTranscript(currentStudentResolver.currentStudentId(), academicYearId, true);
    }

    @Transactional(readOnly = true)
    public ResStudentAnnualTranscriptDTO getAnnualTranscript(Long studentId, Long academicYearId) {
        return getAnnualTranscript(studentId, academicYearId, false);
    }

    @Transactional(readOnly = true)
    public ResTranscriptCalculationStatusDTO getMyTermCalculationStatus(Long semesterId) {
        return getTermCalculationStatus(currentStudentResolver.currentStudentId(), semesterId, true);
    }

    @Transactional(readOnly = true)
    public ResTranscriptCalculationStatusDTO getTermCalculationStatus(Long studentId, Long semesterId) {
        return getTermCalculationStatus(studentId, semesterId, false);
    }

    @Transactional(readOnly = true)
    public ResTranscriptCalculationStatusDTO getMyAnnualCalculationStatus(Long academicYearId) {
        return getAnnualCalculationStatus(currentStudentResolver.currentStudentId(), academicYearId, true);
    }

    @Transactional(readOnly = true)
    public ResTranscriptCalculationStatusDTO getAnnualCalculationStatus(Long studentId, Long academicYearId) {
        return getAnnualCalculationStatus(studentId, academicYearId, false);
    }

    private ResStudentTermTranscriptDTO getTermTranscript(Long studentId, Long semesterId, boolean self) {
        StudentTermTranscript term = termTranscriptRepository.findByStudentIdAndSemesterId(studentId, semesterId)
                .orElseThrow(() -> notFound("Không tìm thấy bảng điểm học kỳ của học sinh"));
        StudentAnnualTranscript annual = annualTranscriptRepository.findById(term.getAnnualTranscriptId())
                .filter(value -> value.getStudentId().equals(studentId))
                .orElseThrow(() -> notFound("Không tìm thấy bảng điểm năm học của học sinh"));
        Semester semester = semesterRepository.findById(semesterId)
                .filter(value -> value.getAcademicYearId().equals(annual.getAcademicYearId()))
                .orElseThrow(() -> notFound("Học kỳ không thuộc năm học của bảng điểm"));
        List<StudentSubjectTermResult> results = termResultRepository
                .findAllByTermTranscriptIdOrderBySubjectIdAsc(term.getId());
        Map<Long, com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject> classSubjects = responseSupport
                .findClassSubjects(results.stream()
                .map(StudentSubjectTermResult::getClassSubjectId).toList());
        if (!self) {
            accessGuard.assertCanRead(studentId, annual.getAcademicYearId(), List.of(semester), classSubjects.values());
        }
        return new ResStudentTermTranscriptDTO(term.getStudentId(), annual.getAcademicYearId(),
                term.getSemesterId(), term.getCalculationStatus(), term.getSourceVersion(), term.getCalculatedVersion(),
                term.getCalculatedAt(),
                term.getDtbhk(), responseSupport.findTransferNotes(studentId, annual.getAcademicYearId()),
                termResponseMapper.map(studentId, results, classSubjects));
    }

    private ResStudentAnnualTranscriptDTO getAnnualTranscript(Long studentId, Long academicYearId, boolean self) {
        StudentAnnualTranscript annual = annualTranscriptRepository
                .findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .orElseThrow(() -> notFound("Không tìm thấy bảng điểm năm học của học sinh"));
        List<Semester> semesters = semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);
        List<StudentSubjectAnnualResult> annualResults = annualResultRepository
                .findAllByAnnualTranscriptIdOrderBySubjectIdAsc(annual.getId());
        Map<Long, StudentSubjectTermResult> termResults = termResultRepository.findAllById(annualResults.stream()
                .flatMap(result -> java.util.stream.Stream.of(result.getHk1TermResultId(), result.getHk2TermResultId()))
                .filter(java.util.Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(StudentSubjectTermResult::getId, Function.identity()));
        Map<Long, com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject> classSubjects = responseSupport
                .findClassSubjects(termResults.values().stream()
                .map(StudentSubjectTermResult::getClassSubjectId).toList());
        if (!self) {
            accessGuard.assertCanRead(studentId, academicYearId, semesters, classSubjects.values());
        }
        return new ResStudentAnnualTranscriptDTO(annual.getStudentId(), annual.getAcademicYearId(),
                annual.getCalculationStatus(), annual.getSourceVersion(), annual.getCalculatedVersion(),
                annual.getCalculatedAt(), annual.getRegularDtbcn(), annual.getFinalDtbcn(), annual.getResultSource(),
                annual.getLastCalculationTaskId(),
                responseSupport.findTransferNotes(studentId, academicYearId),
                responseSupport.mapAnnualResults(annualResults, termResults));
    }

    private ResTranscriptCalculationStatusDTO getTermCalculationStatus(
            Long studentId, Long semesterId, boolean self) {
        StudentTermTranscript term = termTranscriptRepository.findByStudentIdAndSemesterId(studentId, semesterId)
                .orElseThrow(() -> notFound("Không tìm thấy trạng thái bảng điểm học kỳ của học sinh"));
        StudentAnnualTranscript annual = annualTranscriptRepository.findById(term.getAnnualTranscriptId())
                .filter(value -> value.getStudentId().equals(studentId))
                .orElseThrow(() -> notFound("Không tìm thấy bảng điểm năm học của học sinh"));
        Semester semester = semesterRepository.findById(semesterId)
                .filter(value -> value.getAcademicYearId().equals(annual.getAcademicYearId()))
                .orElseThrow(() -> notFound("Học kỳ không thuộc năm học của bảng điểm"));
        if (!self) {
            accessGuard.assertCanRead(studentId, annual.getAcademicYearId(), List.of(semester), List.of());
        }
        return statusResponse(studentId, semester.getAcademicYearId(), semesterId,
                term.getCalculationStatus(), term.getSourceVersion(), term.getCalculatedVersion(),
                term.getCalculatedAt(), term.getLastError());
    }

    private ResTranscriptCalculationStatusDTO getAnnualCalculationStatus(
            Long studentId, Long academicYearId, boolean self) {
        StudentAnnualTranscript annual = annualTranscriptRepository
                .findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .orElseThrow(() -> notFound("Không tìm thấy trạng thái bảng điểm năm học của học sinh"));
        List<Semester> semesters = semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);
        if (!self) {
            accessGuard.assertCanRead(studentId, academicYearId, semesters, List.of());
        }
        return statusResponse(studentId, academicYearId, null,
                annual.getCalculationStatus(), annual.getSourceVersion(), annual.getCalculatedVersion(),
                annual.getCalculatedAt(), annual.getLastError());
    }

    private ResTranscriptCalculationStatusDTO statusResponse(
            Long studentId,
            Long academicYearId,
            Long semesterId,
            CalculationStatus status,
            Long sourceVersion,
            Long calculatedVersion,
            LocalDateTime calculatedAt,
            String lastError) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> notFound("Không tìm thấy học sinh"));
        boolean upToDate = status == CalculationStatus.FINISH
                && sourceVersion != null && sourceVersion.equals(calculatedVersion);
        return new ResTranscriptCalculationStatusDTO(
                studentId,
                student.getStudentCode(),
                academicYearId,
                semesterId,
                status,
                sourceVersion,
                calculatedVersion,
                upToDate,
                calculatedAt,
                lastError);
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }
}
