package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentSubjectTermResult;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentTermTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentSubjectTermResultRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentTermTranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class ClassTermTranscriptReader {

    private final ClassTranscriptScopeReader scopeReader;
    private final ClassTranscriptRosterReader rosterReader;
    private final StudentTermTranscriptRepository termTranscriptRepository;
    private final StudentSubjectTermResultRepository termResultRepository;
    private final TranscriptTermResponseMapper termMapper;

    /* default */ ResClassTermTranscriptDTO read(Long classId, Long semesterId) {
        ClassTranscriptScopeReader.Scope scope = scopeReader.forTerm(classId, semesterId);
        SchoolClass schoolClass = scope.schoolClass();

        ClassTranscriptRosterReader.Roster roster = rosterReader.read(classId);
        if (roster.studentIds().isEmpty()) {
            return emptyResponse(classId, schoolClass, semesterId);
        }

        List<Long> studentIds = roster.studentIds();
        List<StudentTermTranscript> transcripts = termTranscriptRepository
                .findAllBySemesterIdAndStudentIdIn(semesterId, studentIds);
        Map<Long, StudentTermTranscript> transcriptsByStudent = transcripts.stream()
                .collect(Collectors.toMap(StudentTermTranscript::getStudentId, transcript -> transcript));
        Map<Long, List<StudentSubjectTermResult>> resultsByTranscript = findResults(transcripts);
        List<ResClassTermTranscriptDTO.ClassTermStudentRowDTO> rows = new ArrayList<>();
        for (Long studentId : studentIds) {
            rows.add(toStudentRow(studentId, roster.students().get(studentId), transcriptsByStudent.get(studentId),
                    resultsByTranscript));
        }
        return new ResClassTermTranscriptDTO(
                classId, schoolClass.getClassCode(), schoolClass.getClassName(), schoolClass.getAcademicYearId(),
                semesterId, rows);
    }

    private Map<Long, List<StudentSubjectTermResult>> findResults(List<StudentTermTranscript> transcripts) {
        List<Long> transcriptIds = transcripts.stream().map(StudentTermTranscript::getId).toList();
        List<StudentSubjectTermResult> results = transcriptIds.isEmpty() ? List.of()
                : termResultRepository.findAllByTermTranscriptIdInOrderBySubjectIdAsc(transcriptIds);
        return results.stream().collect(Collectors.groupingBy(StudentSubjectTermResult::getTermTranscriptId));
    }

    private ResClassTermTranscriptDTO.ClassTermStudentRowDTO toStudentRow(
            Long studentId, ClassTranscriptRosterReader.StudentSummary student, StudentTermTranscript transcript,
            Map<Long, List<StudentSubjectTermResult>> resultsByTranscript) {
        String studentCode = student == null ? "" : student.studentCode();
        String fullName = student == null ? "" : student.fullName();
        if (transcript == null) {
            return new ResClassTermTranscriptDTO.ClassTermStudentRowDTO(
                    studentId, studentCode, fullName, null, null, List.of());
        }
        List<StudentSubjectTermResult> results = resultsByTranscript.getOrDefault(transcript.getId(), List.of());
        List<ResStudentTermTranscriptDTO.ResTermSubjectResultDTO> subjects = termMapper.map(studentId, results);
        return new ResClassTermTranscriptDTO.ClassTermStudentRowDTO(
                studentId, studentCode, fullName, transcript.getCalculationStatus(), transcript.getDtbhk(), subjects);
    }

    private ResClassTermTranscriptDTO emptyResponse(Long classId, SchoolClass schoolClass, Long semesterId) {
        return new ResClassTermTranscriptDTO(
                classId, schoolClass.getClassCode(), schoolClass.getClassName(), schoolClass.getAcademicYearId(),
                semesterId, List.of());
    }

}
