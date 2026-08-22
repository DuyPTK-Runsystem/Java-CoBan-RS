package com.JavaTraining.BaiTap_RS.teacher.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.service.AcademicCatalogAuditService;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.requests.ReqCreateTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.requests.ReqUpdateTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.response.ResTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository;
    private final AcademicCatalogAuditService auditService;

    public TeacherService(
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            HomeroomAssignmentRepository homeroomAssignmentRepository,
            SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository,
            AcademicCatalogAuditService auditService) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.subjectTeachingAssignmentRepository = subjectTeachingAssignmentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ResTeacherDTO> listTeachers(TeacherStatus status) {
        List<Teacher> teachers = status == null
                ? teacherRepository.findAllByOrderByTeacherCodeAsc()
                : teacherRepository.findAllByStatusOrderByTeacherCodeAsc(status);
        return teachers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ResTeacherDTO getTeacher(Long id) {
        return toResponse(findTeacher(id));
    }

    @Transactional
    public ResTeacherDTO createTeacher(ReqCreateTeacherDTO request) {
        validateUserLink(request.userId(), null);
        if (teacherRepository.existsByTeacherCode(request.teacherCode())) {
            throw conflict("Mã giáo viên đã tồn tại");
        }
        Teacher teacher = new Teacher(
                request.userId(),
                request.teacherCode(),
                request.teacherName(),
                request.dateOfBirth(),
                request.gender(),
                request.phone(),
                request.email(),
                request.department(),
                request.joinDate(),
                request.status());
        Teacher saved = teacherRepository.save(teacher);
        auditService.writeAudit("TEACHER_CREATED", "teacher", saved.getId(), null, teacherData(saved));
        return toResponse(saved);
    }

    @Transactional
    public ResTeacherDTO updateTeacher(Long id, ReqUpdateTeacherDTO request) {
        Teacher teacher = findTeacher(id);
        validateUserLink(request.userId(), id);
        if (teacherRepository.existsByTeacherCodeAndIdNot(request.teacherCode(), id)) {
            throw conflict("Mã giáo viên đã tồn tại");
        }
        Map<String, Object> beforeData = teacherData(teacher);
        teacher.setUserId(request.userId());
        teacher.setTeacherCode(request.teacherCode());
        teacher.setTeacherName(request.teacherName());
        teacher.setDateOfBirth(request.dateOfBirth());
        teacher.setGender(request.gender());
        teacher.setPhone(request.phone());
        teacher.setEmail(request.email());
        teacher.setDepartment(request.department());
        teacher.setJoinDate(request.joinDate());
        teacher.setStatus(request.status());
        String action = beforeData.get("status").equals(teacher.getStatus().name())
                ? "TEACHER_UPDATED"
                : "TEACHER_STATUS_CHANGED";
        auditService.writeAudit(action, "teacher", teacher.getId(), beforeData, teacherData(teacher));
        return toResponse(teacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = findTeacher(id);
        if (homeroomAssignmentRepository.existsByTeacherId(id)
                || subjectTeachingAssignmentRepository.existsByTeacherId(id)) {
            throw conflict("Không thể xóa giáo viên đã phát sinh phân công");
        }
        teacherRepository.delete(teacher);
    }

    private void validateUserLink(Long userId, Long teacherId) {
        if (userId == null) {
            return;
        }
        if (!userRepository.existsById(userId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
        }
        boolean linked = teacherId == null
                ? teacherRepository.existsByUserId(userId)
                : teacherRepository.existsByUserIdAndIdNot(userId, teacherId);
        if (linked) {
            throw conflict("Tài khoản đã liên kết với giáo viên khác");
        }
    }

    private Teacher findTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giáo viên"));
    }

    private ResTeacherDTO toResponse(Teacher teacher) {
        return new ResTeacherDTO(
                teacher.getId(),
                teacher.getUserId(),
                teacher.getTeacherCode(),
                teacher.getTeacherName(),
                teacher.getDateOfBirth(),
                teacher.getGender(),
                teacher.getPhone(),
                teacher.getEmail(),
                teacher.getDepartment(),
                teacher.getJoinDate(),
                teacher.getStatus());
    }

    private Map<String, Object> teacherData(Teacher teacher) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", teacher.getId());
        data.put("userId", teacher.getUserId());
        data.put("teacherCode", teacher.getTeacherCode());
        data.put("teacherName", teacher.getTeacherName());
        data.put("dateOfBirth", teacher.getDateOfBirth());
        data.put("gender", teacher.getGender());
        data.put("phone", teacher.getPhone());
        data.put("email", teacher.getEmail());
        data.put("department", teacher.getDepartment());
        data.put("joinDate", teacher.getJoinDate());
        data.put("status", teacher.getStatus().name());
        return data;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
