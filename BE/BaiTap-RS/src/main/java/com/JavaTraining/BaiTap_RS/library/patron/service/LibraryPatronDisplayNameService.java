package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryPatronDisplayNameService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public Map<Long, String> displayNames(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (Student student : studentRepository.findAllByUserIdIn(userIds)) {
            result.put(student.getUserId(), student.getStudentName());
        }
        for (Teacher teacher : teacherRepository.findAllByUserIdIn(userIds)) {
            result.put(teacher.getUserId(), teacher.getTeacherName());
        }
        return result;
    }

    public Map<Long, String> usernames(List<Long> userIds) {
        Map<Long, String> result = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> result.put(user.getId(), user.getUsername()));
        return result;
    }

    public String displayName(Long userId) {
        Map<Long, String> names = displayNames(List.of(userId));
        return names.getOrDefault(userId, userRepository.findById(userId).map(User::getUsername).orElse(""));
    }
}
