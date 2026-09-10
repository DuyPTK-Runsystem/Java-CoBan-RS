package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectAnnualResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectAnnualResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class ClassAnnualResultReader {

    private final StudentSubjectAnnualResultRepository annualResultRepository;
    private final StudentSubjectTermResultRepository termResultRepository;
    private final TranscriptResponseSupport responseSupport;

    /* default */ Map<Long, List<ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO>> read(
            List<StudentAnnualTranscript> transcripts) {
        List<Long> transcriptIds = transcripts.stream().map(StudentAnnualTranscript::getId).toList();
        List<StudentSubjectAnnualResult> results = transcriptIds.isEmpty() ? List.of()
                : annualResultRepository.findAllByAnnualTranscriptIdInOrderBySubjectIdAsc(transcriptIds);
        Map<Long, List<StudentSubjectAnnualResult>> resultsByTranscript = results.stream()
                .collect(Collectors.groupingBy(StudentSubjectAnnualResult::getAnnualTranscriptId));
        Map<Long, StudentSubjectTermResult> termResults = findTermResults(results);
        return transcripts.stream().collect(Collectors.toMap(StudentAnnualTranscript::getId,
                transcript -> responseSupport.mapAnnualResults(
                        resultsByTranscript.getOrDefault(transcript.getId(), List.of()), termResults)));
    }

    private Map<Long, StudentSubjectTermResult> findTermResults(List<StudentSubjectAnnualResult> results) {
        List<Long> termResultIds = results.stream()
                .flatMap(result -> Stream.of(result.getHk1TermResultId(), result.getHk2TermResultId()))
                .filter(Objects::nonNull).distinct().toList();
        return termResultIds.isEmpty() ? Map.of() : termResultRepository.findAllById(termResultIds).stream()
                .collect(Collectors.toMap(StudentSubjectTermResult::getId, Function.identity()));
    }
}
