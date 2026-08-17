package com.JavaTraining.BaiTap_RS.user.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.security.JwtTokenService;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.requests.ReqLoginUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.requests.ReqRegisterUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.response.ResLoginUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.DTOs.response.ResUserDTO;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public ResUserDTO register(ReqRegisterUserDTO request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }
        User user = new User(request.username(), passwordEncoder.encode(request.password()));
        return toUserDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public ResLoginUserDTO login(ReqLoginUserDTO request) {
        Authentication authentication = authenticate(request);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtTokenService.createAccessToken(principal);
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không hợp lệ"));
        return new ResLoginUserDTO(accessToken, toUserDTO(user));
    }

    @Transactional(readOnly = true)
    public ResUserDTO getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));
        return toUserDTO(user);
    }

    public ResUserDTO toUserDTO(User user) {
        return new ResUserDTO(
                user.getId(),
                user.getUsername(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy());
    }

    private Authentication authenticate(ReqLoginUserDTO request) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException exception) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không hợp lệ", exception);
        }
    }
}
