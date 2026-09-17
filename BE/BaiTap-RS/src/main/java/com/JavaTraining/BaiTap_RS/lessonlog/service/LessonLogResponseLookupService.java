package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogResponseLookupService {
    private final LessonLogPolicyService policyService;
    private final SchoolClassRepository schoolClasses;
    private final SubjectRepository subjects;
    private final TeacherRepository teachers;
    private final ObjectMapper objectMapper;

    public LessonLogEntryResponseMapper.DisplayValues displayValues(LessonLogEntry entry) {
        try {
            JsonNode node = mapper().readTree(entry.getSourceSnapshotJson());
            return new LessonLogEntryResponseMapper.DisplayValues(text(node, "className", className(entry.getClassId())),
                    text(node, "subjectName", subjectName(entry.getSubjectId())),
                    text(node, "teacherName", teacherName(entry.getAssignedTeacherId())),
                    text(node, "functionalRoomName", null));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return new LessonLogEntryResponseMapper.DisplayValues(className(entry.getClassId()), subjectName(entry.getSubjectId()),
                    teacherName(entry.getAssignedTeacherId()), null);
        }
    }

    public LessonLogPolicy policy(Long policyId) {
        return policyId == null ? null : optional(policyService.findById(policyId)).orElse(null);
    }

    private ObjectMapper mapper() {
        return objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    private String className(Long classId) {
        return optional(schoolClasses.findById(classId)).map(SchoolClass::getClassName).orElse(null);
    }

    private String subjectName(Long subjectId) {
        return optional(subjects.findById(subjectId)).map(Subject::getName).orElse(null);
    }

    private String teacherName(Long teacherId) {
        return optional(teachers.findById(teacherId)).map(Teacher::getTeacherName).orElse(null);
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private <T> Optional<T> optional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }
}
