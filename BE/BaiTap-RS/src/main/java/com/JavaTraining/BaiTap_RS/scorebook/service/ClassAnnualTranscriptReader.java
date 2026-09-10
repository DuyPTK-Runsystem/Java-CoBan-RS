package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentAnnualTranscript;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentAnnualTranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class ClassAnnualTranscriptReader {

    private final ClassTranscriptScopeReader scopeReader;
    private final ClassTranscriptRosterReader rosterReader;
    private final StudentAnnualTranscriptRepository annualTranscriptRepository;
    private final ClassAnnualResultReader annualResultReader;

    /* default */ ResClassAnnualTranscriptDTO read(Long classId, Long academicYearId) {
        ClassTranscriptScopeReader.Scope scope = scopeReader.forAnnual(classId, academicYearId);
        SchoolClass schoolClass = scope.schoolClass();

        ClassTranscriptRosterReader.Roster roster = rosterReader.read(classId);
        if (roster.studentIds().isEmpty()) {
            return emptyResponse(classId, schoolClass, academicYearId);
        }

        List<Long> studentIds = roster.studentIds();
        List<StudentAnnualTranscript> transcripts = annualTranscriptRepository
                .findAllByAcademicYearIdAndStudentIdIn(academicYearId, studentIds);
        Map<Long, StudentAnnualTranscript> transcriptsByStudent = transcripts.stream()
                .collect(Collectors.toMap(StudentAnnualTranscript::getStudentId, transcript -> transcript));
        Map<Long, List<ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO>> subjectsByTranscript =
                annualResultReader.read(transcripts);

        List<ResClassAnnualTranscriptDTO.ClassAnnualStudentRowDTO> rows = new ArrayList<>();
        for (Long studentId : studentIds) {
            rows.add(toStudentRow(studentId, roster.students().get(studentId), transcriptsByStudent.get(studentId),
                    subjectsByTranscript));
        }
        return new ResClassAnnualTranscriptDTO(
                classId, schoolClass.getClassCode(), schoolClass.getClassName(), academicYearId, rows);
    }

    private ResClassAnnualTranscriptDTO.ClassAnnualStudentRowDTO toStudentRow(
            Long studentId, ClassTranscriptRosterReader.StudentSummary student, StudentAnnualTranscript transcript,
            Map<Long, List<ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO>> subjectsByTranscript) {
        String studentCode = student == null ? "" : student.studentCode();
        String fullName = student == null ? "" : student.fullName();
        if (transcript == null) {
            return new ResClassAnnualTranscriptDTO.ClassAnnualStudentRowDTO(
                    studentId, studentCode, fullName, null, null, null, null, List.of());
        }
        List<ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO> subjects = subjectsByTranscript
                .getOrDefault(transcript.getId(), List.of());
        return new ResClassAnnualTranscriptDTO.ClassAnnualStudentRowDTO(
                studentId, studentCode, fullName, transcript.getCalculationStatus(), transcript.getRegularDtbcn(),
                transcript.getFinalDtbcn(), transcript.getResultSource(), subjects);
    }

    private ResClassAnnualTranscriptDTO emptyResponse(Long classId, SchoolClass schoolClass, Long academicYearId) {
        return new ResClassAnnualTranscriptDTO(
                classId, schoolClass.getClassCode(), schoolClass.getClassName(), academicYearId, List.of());
    }

}
