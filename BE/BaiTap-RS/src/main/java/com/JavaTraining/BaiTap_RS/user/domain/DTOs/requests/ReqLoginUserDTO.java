package com.JavaTraining.BaiTap_RS.user.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReqLoginUserDTO(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(max = 20, message = "Tên đăng nhập tối đa 20 ký tự")
        @Pattern(regexp = "\\A\\p{ASCII}+\\z", message = "Tên đăng nhập chỉ được dùng ký tự 1-byte")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 15, message = "Mật khẩu phải từ 6 đến 15 ký tự")
        @Pattern(regexp = "\\A\\p{ASCII}+\\z", message = "Mật khẩu chỉ được dùng ký tự 1-byte")
        String password) {
}
