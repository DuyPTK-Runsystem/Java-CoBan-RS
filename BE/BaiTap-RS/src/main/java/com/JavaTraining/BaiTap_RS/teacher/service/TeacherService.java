package com.JavaTraining.BaiTap_RS.teacher.service;

import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.service.AcademicCatalogAuditService;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.requests.ReqCreateTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.requests.ReqUpdateTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.DTOs.response.ResTeacherDTO;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.RoleRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final AcademicCatalogAuditService auditService;
    private final TeacherServiceSupport support;

    public TeacherService(TeacherRepository teacherRepository, UserRepository userRepository,
            RoleRepository roleRepository, HomeroomAssignmentRepository homeroomAssignmentRepository,
            SubjectTeachingAssignmentRepository subjectTeachingAssignmentRepository,
            AcademicCatalogAuditService auditService) {
        this.teacherRepository = teacherRepository;
        this.auditService = auditService;
        this.support = new TeacherServiceSupport(teacherRepository, userRepository, roleRepository,
                homeroomAssignmentRepository, subjectTeachingAssignmentRepository);
    }

    @Transactional(readOnly = true)
    public List<ResTeacherDTO> listTeachers(TeacherStatus status) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ TeacherService.class, "TeacherService.listTeachers");
        List<Teacher> teachers = status == null ? teacherRepository.findAllByOrderByTeacherCodeAsc()
                : teacherRepository.findAllByStatusOrderByTeacherCodeAsc(status);
        return teachers.stream().map(support::response).toList();
    }

    @Transactional(readOnly = true)
    public ResTeacherDTO getTeacher(Long id) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ TeacherService.class, "TeacherService.getTeacher");
        return support.response(support.find(id));
    }

    @Transactional
    public ResTeacherDTO createTeacher(ReqCreateTeacherDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ TeacherService.class, "TeacherService.createTeacher");
        support.validateUserLink(request.userId(), null);
        if (teacherRepository.existsByTeacherCode(request.teacherCode())) {
            throw support.conflict("Mã giáo viên đã tồn tại");
        }
        User user = support.assignTeacherRole(request.userId());
        Teacher teacher = new Teacher(user == null ? null : user.getId(), request.teacherCode(), request.teacherName(),
                request.dateOfBirth(), request.gender(), request.phone(), request.email(), request.department(),
                request.joinDate(), request.status());
        Teacher saved = teacherRepository.save(teacher);
        auditService.writeAudit("TEACHER_CREATED", "teacher", saved.getId(), null, support.data(saved));
        return support.response(saved);
    }

    @Transactional
    public ResTeacherDTO updateTeacher(Long id, ReqUpdateTeacherDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ TeacherService.class, "TeacherService.updateTeacher");
        Teacher teacher = support.find(id);
        support.validateUserLink(request.userId(), id);
        if (teacherRepository.existsByTeacherCodeAndIdNot(request.teacherCode(), id)) {
            throw support.conflict("Mã giáo viên đã tồn tại");
        }
        Map<String, Object> before = support.data(teacher);
        teacher.setUserId(request.userId()); teacher.setTeacherCode(request.teacherCode());
        teacher.setTeacherName(request.teacherName()); teacher.setDateOfBirth(request.dateOfBirth());
        teacher.setGender(request.gender()); teacher.setPhone(request.phone()); teacher.setEmail(request.email());
        teacher.setDepartment(request.department()); teacher.setJoinDate(request.joinDate());
        teacher.setStatus(request.status());
        String action = before.get("status").equals(teacher.getStatus().name())
                ? "TEACHER_UPDATED" : "TEACHER_STATUS_CHANGED";
        auditService.writeAudit(action, "teacher", teacher.getId(), before, support.data(teacher));
        return support.response(teacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ TeacherService.class, "TeacherService.deleteTeacher");
        if (support.hasAssignments(id)) {
            throw new AppException(HttpStatus.CONFLICT, "Không thể xóa giáo viên đã phát sinh phân công");
        }
        teacherRepository.delete(support.find(id));
    }
}
