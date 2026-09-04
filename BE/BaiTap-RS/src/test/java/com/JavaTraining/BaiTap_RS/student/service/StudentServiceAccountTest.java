package com.JavaTraining.BaiTap_RS.student.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.requests.ReqCreateStudentV3DTO;
import com.JavaTraining.BaiTap_RS.student.domain.DTOs.response.ResStudentWithAccountDTO;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.RoleRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentServiceAccountTest {

    private static final String STUDENT_CODE = "STU1234567";
    private static final String STUDENT_NAME = "Khánh Duy";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String EXPLICIT_USERNAME = "student01";
    private static final String EXPLICIT_PASSWORD = "secret12";

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StudentUsernameGenerator usernameGenerator;

    private StudentAccountService studentService;
    private final AtomicReference<User> savedUser = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        studentService = new StudentAccountService(
                studentRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                usernameGenerator);
    }

    @Test
    void createStudentWithAccountUsesDefaultsAndAssignsStudentRole() {
        Role studentRole = new Role(ROLE_STUDENT, "Student", "Học sinh");
        prepareCreate(studentRole);
        Mockito.when(usernameGenerator.generate(STUDENT_NAME, STUDENT_CODE)).thenReturn("khanhduy1234567");
        Mockito.when(passwordEncoder.encode("12345678")).thenReturn("hashed");

        ResStudentWithAccountDTO response = studentService.createStudentWithAccount(request(null, null));

        Assertions.assertEquals(
                "khanhduy1234567|10|" + ROLE_STUDENT + "|true",
                response.account().username()
                        + "|"
                        + response.account().userId()
                        + "|"
                        + response.account().role()
                        + "|"
                        + savedUser.get().getRoles().contains(studentRole),
                "default account should be linked and assigned the STUDENT role");
    }

    @Test
    void createStudentWithAccountUsesExplicitCredentials() {
        Role studentRole = new Role(ROLE_STUDENT, "Student", "Học sinh");
        prepareCreate(studentRole);
        Mockito.when(passwordEncoder.encode(EXPLICIT_PASSWORD)).thenReturn("hashed");

        ResStudentWithAccountDTO response = studentService.createStudentWithAccount(
                request(EXPLICIT_USERNAME, EXPLICIT_PASSWORD));

        Assertions.assertEquals(
                EXPLICIT_USERNAME,
                response.account().username(),
                "explicit username should be preserved");
    }

    @Test
    void createStudentWithAccountRejectsDuplicateUsernameBeforeCreatingUser() {
        Mockito.when(studentRepository.existsByStudentCode(STUDENT_CODE)).thenReturn(false);
        Mockito.when(userRepository.existsByUsername(EXPLICIT_USERNAME)).thenReturn(true);

        Assertions.assertThrows(
                AppException.class,
                () -> {
                    try {
                        studentService.createStudentWithAccount(request(EXPLICIT_USERNAME, EXPLICIT_PASSWORD));
                    } catch (AppException exception) {
                        if (exception.getStatus() != HttpStatus.CONFLICT) {
                            throw new AssertionError("duplicate username should conflict", exception);
                        }
                        throw exception;
                    }
                });
    }

    @Test
    void createStudentWithAccountRejectsMissingStudentRole() {
        Mockito.when(studentRepository.existsByStudentCode(STUDENT_CODE)).thenReturn(false);
        Mockito.when(userRepository.existsByUsername(EXPLICIT_USERNAME)).thenReturn(false);
        Mockito.when(roleRepository.findByCode(ROLE_STUDENT)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                AppException.class,
                () -> {
                    try {
                        studentService.createStudentWithAccount(request(EXPLICIT_USERNAME, EXPLICIT_PASSWORD));
                    } catch (AppException exception) {
                        if (exception.getStatus() != HttpStatus.INTERNAL_SERVER_ERROR) {
                            throw new AssertionError("missing STUDENT role should be a server error", exception);
                        }
                        throw exception;
                    }
                });
    }

    private void prepareCreate(Role role) {
        Mockito.when(studentRepository.existsByStudentCode(STUDENT_CODE)).thenReturn(false);
        Mockito.when(userRepository.existsByUsername(Mockito.any(String.class))).thenReturn(false);
        Mockito.when(roleRepository.findByCode(ROLE_STUDENT)).thenReturn(Optional.of(role));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 10L);
            savedUser.set(user);
            return user;
        });
        Mockito.when(studentRepository.save(Mockito.any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            ReflectionTestUtils.setField(student, "id", 1L);
            return student;
        });
    }

    private ReqCreateStudentV3DTO request(String username, String password) {
        return new ReqCreateStudentV3DTO(
                STUDENT_CODE,
                STUDENT_NAME,
                LocalDate.of(2010, 5, 10),
                "TPHCM",
                8.5,
                username,
                password);
    }
}
