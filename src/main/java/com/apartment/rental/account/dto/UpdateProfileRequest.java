package com.apartment.rental.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String email;

    @NotBlank(message = "Họ và tên không được để trống!")
    @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự!")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải gồm 10 chữ số!")
    private String phone;

    @NotBlank(message = "CCCD/CMND không được để trống!")
    @Pattern(regexp = "^\\d{12}$", message = "Số CCCD phải gồm 12 chữ số!")
    private String identityDocumentNo;
}