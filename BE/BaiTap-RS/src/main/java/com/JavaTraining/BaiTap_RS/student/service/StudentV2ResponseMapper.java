package com.JavaTraining.BaiTap_RS.student.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentV2DTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.repository.StudentClassLookupRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
class StudentV2ResponseMapper {

    private final StudentClassLookupRepository classLookupRepository;
    private final UserRepository userRepository;

    /* default */ StudentV2ResponseMapper(
            StudentClassLookupRepository classLookupRepository, UserRepository userRepository) {
        this.classLookupRepository = classLookupRepository;
        this.userRepository = userRepository;
    }

    /* default */ Map<Long, CurrentClass> currentClasses(Collection<Student> students) {
        List<Long> studentIds = students.stream().map(Student::getId).toList();
        return currentClassesByStudentIds(studentIds);
    }

    private Map<Long, CurrentClass> currentClassesByStudentIds(Collection<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, CurrentClass> result = new HashMap<>();
        classLookupRepository.findActiveClassesByStudentIds(studentIds)
                .forEach(row -> addCurrentClass(result, row));
        return result;
    }

    /* default */ CurrentClass currentClass(Long studentId) {
        return currentClassesByStudentIds(List.of(studentId)).get(studentId);
    }

    /* default */ Map<Long, User> users(Collection<Student> students) {
        List<Long> userIds = students.stream()
                .map(Student::getUserId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> users = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> users.put(user.getId(), user));
        return users;
    }

    /* default */ User user(Long userId) {
        return userId == null ? null : userRepository.findById(userId).orElse(null);
    }

    /* default */ ResStudentV2DTO response(Student student, CurrentClass currentClass, User user) {
        StudentInfo info = student.getStudentInfo();
        return new ResStudentV2DTO(
                student.getId(),
                student.getStudentCode(),
                student.getStudentName(),
                info == null ? null : info.getDateOfBirth(),
                info == null ? null : info.getAddress(),
                info == null ? null : info.getGender(),
                student.getStatus(),
                currentClass == null ? null : currentClass.id(),
                currentClass == null ? null : currentClass.code(),
                user == null ? null : new ResStudentV2DTO.Account(user.getId(), user.getUsername(), "STUDENT"));
    }

    private void addCurrentClass(Map<Long, CurrentClass> result, Object... row) {
        if (row[0] instanceof Long studentId && row[1] instanceof Long classId && row[2] instanceof String classCode) {
            result.put(studentId, new CurrentClass(classId, classCode));
        }
    }

    /* default */ record CurrentClass(Long id, String code) {
    }
}
