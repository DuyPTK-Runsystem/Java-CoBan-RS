package com.JavaTraining.BaiTap_RS.student.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentV3DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentWithAccountDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentInfo;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.RoleRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAccountService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentUsernameGenerator studentUsernameGenerator;

    public StudentAccountService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            StudentUsernameGenerator studentUsernameGenerator) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.studentUsernameGenerator = studentUsernameGenerator;
    }

    @Transactional
    public ResStudentWithAccountDTO createStudentWithAccount(ReqCreateStudentV3DTO request) {
        if (studentRepository.existsByStudentCode(request.studentCode())) {
            throw new AppException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại");
        }
        String username = request.username() == null
                ? studentUsernameGenerator.generate(request.studentName(), request.studentCode())
                : request.username();
        if (userRepository.existsByUsername(username)) {
            throw new AppException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }
        Role studentRole = roleRepository.findByCode("STUDENT")
                .orElseThrow(() -> new AppException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không tìm thấy role STUDENT trong hệ thống"));
        User user = new User(username, passwordEncoder.encode(
                request.password() == null ? "12345678" : request.password()));
        user.addRole(studentRole);
        User savedUser = userRepository.save(user);

        Student student = new Student(request.studentName(), request.studentCode());
        student.setUserId(savedUser.getId());
        student.assignInfo(new StudentInfo(request.dateOfBirth(), request.address(), request.averageScore()));
        Student savedStudent = studentRepository.save(student);
        return toResponse(savedStudent, savedUser);
    }

    private ResStudentWithAccountDTO toResponse(Student student, User user) {
        StudentInfo info = student.getStudentInfo();
        return new ResStudentWithAccountDTO(
                student.getId(),
                student.getStudentCode(),
                student.getStudentName(),
                info == null ? null : info.getDateOfBirth(),
                info == null ? null : info.getAddress(),
                info == null ? null : info.getAverageScore(),
                new ResStudentWithAccountDTO.Account(user.getId(), user.getUsername(), "STUDENT"));
    }
}
