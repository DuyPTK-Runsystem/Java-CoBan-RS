package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassTermTranscriptDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class ClassTranscriptQueryService {

    private final ClassTermTranscriptReader termReader;
    private final ClassAnnualTranscriptReader annualReader;

    @Transactional(readOnly = true)
    public ResClassTermTranscriptDTO getClassTermTranscript(Long classId, Long semesterId) {
        trace("getClassTermTranscript");
        return termReader.read(classId, semesterId);
    }

    @Transactional(readOnly = true)
    public ResClassAnnualTranscriptDTO getClassAnnualTranscript(Long classId, Long academicYearId) {
        trace("getClassAnnualTranscript");
        return annualReader.read(classId, academicYearId);
    }

    private void trace(String operation) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ ClassTranscriptQueryService.class,
                "ClassTranscriptQueryService." + operation);
    }
}
