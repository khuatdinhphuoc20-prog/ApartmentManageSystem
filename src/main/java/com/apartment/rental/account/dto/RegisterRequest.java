package com.apartment.rental.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống!")
    @Email(message = "Email không hợp lệ!")
    @Size(max = 254, message = "Email không được vượt quá 254 ký tự!")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự!")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống!")
    private String confirmPassword;

    @NotBlank(message = "Họ và tên không được để trống!")
    @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự!")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải gồm đúng 10 chữ số!")
    private String phone;

    @NotBlank(message = "CCCD/CMND không được để trống!")
    @Pattern(regexp = "^\\d{12}$", message = "Số CCCD phải gồm đúng 12 chữ số!")
    private String identityDocumentNo;
}