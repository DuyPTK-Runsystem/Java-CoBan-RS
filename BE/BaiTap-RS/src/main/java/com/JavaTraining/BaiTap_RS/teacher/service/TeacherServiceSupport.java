package com.JavaTraining.BaiTap_RS.teacher.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.response.ResTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.RoleRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.http.HttpStatus;

final class TeacherServiceSupport {
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;

    public TeacherServiceSupport(TeacherRepository teacherRepository, UserRepository userRepository,
            RoleRepository roleRepository, HomeroomAssignmentRepository homeroomAssignmentRepository,
            SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.subjectTeachingAssignmentRepository = subjectTeachingAssignmentRepository;
    }

    public void validateUserLink(Long userId, Long teacherId) {
        if (userId == null) {
            return;
        }
        if (!userRepository.existsById(userId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
        }
        boolean linked = teacherId == null ? teacherRepository.existsByUserId(userId)
                : teacherRepository.existsByUserIdAndIdNot(userId, teacherId);
        if (linked) {
            throw conflict("Tài khoản đã liên kết với giáo viên khác");
        }
    }

    public User assignTeacherRole(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
        Role role = roleRepository.findByCode("TEACHER").orElseThrow(() -> new AppException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy role TEACHER trong hệ thống"));
        if (user.getRoles().stream().noneMatch(item -> "TEACHER".equals(item.getCode()))) {
            user.addRole(role);
        }
        return user;
    }

    public Teacher find(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giáo viên"));
    }

    public ResTeacherDTO response(Teacher teacher) {
        return new ResTeacherDTO(teacher.getId(), teacher.getUserId(), teacher.getTeacherCode(), teacher.getTeacherName(),
                teacher.getDateOfBirth(), teacher.getGender(), teacher.getPhone(), teacher.getEmail(),
                teacher.getDepartment(), teacher.getJoinDate(), teacher.getStatus());
    }

    public Map<String, Object> data(Teacher teacher) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", teacher.getId()); data.put("userId", teacher.getUserId());
        data.put("teacherCode", teacher.getTeacherCode()); data.put("teacherName", teacher.getTeacherName());
        data.put("dateOfBirth", teacher.getDateOfBirth()); data.put("gender", teacher.getGender());
        data.put("phone", teacher.getPhone()); data.put("email", teacher.getEmail());
        data.put("department", teacher.getDepartment()); data.put("joinDate", teacher.getJoinDate());
        data.put("status", teacher.getStatus().name());
        return data;
    }

    public boolean hasAssignments(Long id) {
        return homeroomAssignmentRepository.existsByTeacherId(id)
                || subjectTeachingAssignmentRepository.existsByTeacherId(id);
    }

    public AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
