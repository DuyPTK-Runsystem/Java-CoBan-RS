package com.JavaTraining.BaiTap_RS.user.service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.security.JwtTokenService;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.requests.ReqLoginUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.requests.ReqRegisterUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.response.ResLoginUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.response.ResUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USERNAME = "student01";
    private static final String PASSWORD = "secret1";
    private static final String HASHED_PASSWORD = "hashed-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, authenticationManager, jwtTokenService);
    }

    @Test
    void registerCreatesUserWhenUsernameIsNewAndPasswordConfirmationMatches() {
        ReqRegisterUserDTO request = new ReqRegisterUserDTO(USERNAME, PASSWORD, PASSWORD);
        AtomicReference<User> savedUser = new AtomicReference<>();
        Mockito.when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        Mockito.when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED_PASSWORD);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            savedUser.set(user);
            return user;
        });

        ResUserDTO response = userService.register(request);

        String actual = savedUser.get().getUsername()
                + "|"
                + savedUser.get().getPassword()
                + "|"
                + response.username();
        Assertions.assertEquals(
                USERNAME + "|" + HASHED_PASSWORD + "|" + USERNAME,
                actual,
                "register should save hashed password and return username");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        ReqRegisterUserDTO request = new ReqRegisterUserDTO(USERNAME, PASSWORD, PASSWORD);
        Mockito.when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        AppException exception = captureAppException(() -> userService.register(request));

        Assertions.assertEquals(
                HttpStatus.CONFLICT.value() + "|Tên đăng nhập đã tồn tại",
                exception.getStatus().value() + "|" + exception.getMessage(),
                "duplicate username should return conflict");
    }

    @Test
    void registerRejectsMismatchedPasswordConfirmation() {
        ReqRegisterUserDTO request = new ReqRegisterUserDTO(USERNAME, PASSWORD, "secret2");

        AppException exception = captureAppException(() -> userService.register(request));

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST.value() + "|Mật khẩu xác nhận không khớp",
                exception.getStatus().value() + "|" + exception.getMessage(),
                "mismatched confirmation should return bad request");
    }

    @Test
    void loginReturnsAccessTokenAndUserInformationWhenCredentialsAreValid() {
        ReqLoginUserDTO request = new ReqLoginUserDTO(USERNAME, PASSWORD);
        User user = userWithId(1L, USERNAME, HASHED_PASSWORD);
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        Mockito.when(jwtTokenService.createAccessToken(principal)).thenReturn("access-token");
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        ResLoginUserDTO response = userService.login(request);

        Assertions.assertEquals(
                "access-token|1|student01",
                response.accessToken() + "|" + response.user().id() + "|" + response.user().username(),
                "login should return token and user summary");
    }

    @Test
    void loginRejectsBadCredentials() {
        ReqLoginUserDTO request = new ReqLoginUserDTO(USERNAME, "wrong-password");
        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        AppException exception = captureAppException(() -> userService.login(request));

        Assertions.assertEquals(
                HttpStatus.UNAUTHORIZED.value() + "|Thông tin đăng nhập không hợp lệ",
                exception.getStatus().value() + "|" + exception.getMessage(),
                "bad credentials should return unauthorized");
    }

    private User userWithId(Long id, String username, String password) {
        User user = new User(username, password);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private AppException captureAppException(Runnable action) {
        try {
            action.run();
            return new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_EXCEPTION");
        } catch (AppException exception) {
            return exception;
        }
    }
}
