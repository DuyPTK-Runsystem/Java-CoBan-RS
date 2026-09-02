package com.JavaTraining.BaiTap_RS.user.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.requests.ReqLoginUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.requests.ReqRegisterUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.response.ResLoginUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.response.ResUserDTO;
import com.JavaTraining.BaiTap_RS.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@SuppressWarnings("PMD.GuardLogStatement")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ApiMessage("Đăng ký người dùng")
    public ResponseEntity<ResUserDTO> register(@Valid @RequestBody ReqRegisterUserDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AuthController.class,
                "AuthController.register");
        ResUserDTO user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    @ApiMessage("Đăng nhập")
    public ResLoginUserDTO login(@Valid @RequestBody ReqLoginUserDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AuthController.class,
                "AuthController.login");
        return userService.login(request);
    }

    @GetMapping("/account")
    @ApiMessage("Lấy thông tin tài khoản")
    public ResUserDTO account(@AuthenticationPrincipal UserPrincipal principal) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AuthController.class,
                "AuthController.account");
        return userService.getCurrentUser(principal);
    }

    @PostMapping("/logout")
    @ApiMessage("Đăng xuất")
    public ResponseEntity<Void> logout() {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AuthController.class,
                "AuthController.logout");
        return ResponseEntity.noContent().build();
    }
}
