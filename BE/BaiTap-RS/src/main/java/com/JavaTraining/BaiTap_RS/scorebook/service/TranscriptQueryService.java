package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResTranscriptCalculationStatusDTO;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class TranscriptQueryService {
    private final TranscriptQuerySupport support;
    private final TranscriptStatusSupport statusSupport;
    private final TranscriptCurrentStudentResolver currentStudentResolver;

    public TranscriptQueryService(StudentAnnualTranscriptRepository annualRepository,
            StudentTermTranscriptRepository termRepository, StudentSubjectTermResultRepository termResultRepository,
            StudentSubjectAnnualResultRepository annualResultRepository, SemesterRepository semesterRepository,
            TranscriptAccessGuard accessGuard, TranscriptTermResponseMapper termMapper,
            TranscriptResponseSupport responseSupport, TranscriptCurrentStudentResolver currentStudentResolver,
            StudentRepository studentRepository) {
        this.support = new TranscriptQuerySupport(annualRepository, termRepository, termResultRepository,
                annualResultRepository, semesterRepository, accessGuard, termMapper, responseSupport);
        this.statusSupport = new TranscriptStatusSupport(annualRepository, termRepository, semesterRepository,
                accessGuard, studentRepository);
        this.currentStudentResolver = currentStudentResolver;
    }

    @Transactional(readOnly = true)
    public ResStudentTermTranscriptDTO getMyTermTranscript(Long semesterId) {
        trace("getMyTermTranscript");
        return support.termTranscript(currentStudentResolver.currentStudentId(), semesterId, true);
    }

    @Transactional(readOnly = true)
    public ResStudentTermTranscriptDTO getTermTranscript(Long studentId, Long semesterId) {
        trace("getTermTranscript");
        return support.termTranscript(studentId, semesterId, false);
    }

    @Transactional(readOnly = true)
    public ResStudentAnnualTranscriptDTO getMyAnnualTranscript(Long academicYearId) {
        trace("getMyAnnualTranscript");
        return support.annualTranscript(currentStudentResolver.currentStudentId(), academicYearId, true);
    }

    @Transactional(readOnly = true)
    public ResStudentAnnualTranscriptDTO getAnnualTranscript(Long studentId, Long academicYearId) {
        trace("getAnnualTranscript");
        return support.annualTranscript(studentId, academicYearId, false);
    }

    @Transactional(readOnly = true)
    public ResTranscriptCalculationStatusDTO getMyTermCalculationStatus(Long semesterId) {
        trace("getMyTermCalculationStatus");
        return statusSupport.termStatus(currentStudentResolver.currentStudentId(), semesterId, true);
    }

    @Transactional(readOnly = true)
    public ResTranscriptCalculationStatusDTO getTermCalculationStatus(Long studentId, Long semesterId) {
        trace("getTermCalculationStatus");
        return statusSupport.termStatus(studentId, semesterId, false);
    }

    @Transactional(readOnly = true)
    public ResTranscriptCalculationStatusDTO getMyAnnualCalculationStatus(Long academicYearId) {
        trace("getMyAnnualCalculationStatus");
        return statusSupport.annualStatus(currentStudentResolver.currentStudentId(), academicYearId, true);
    }

    @Transactional(readOnly = true)
    public ResTranscriptCalculationStatusDTO getAnnualCalculationStatus(Long studentId, Long academicYearId) {
        trace("getAnnualCalculationStatus");
        return statusSupport.annualStatus(studentId, academicYearId, false);
    }

    private void trace(String operation) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ TranscriptQueryService.class,
                "TranscriptQueryService." + operation);
    }
}
