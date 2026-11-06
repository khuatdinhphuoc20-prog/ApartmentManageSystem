package com.apartment.rental.account.controller;

import com.apartment.rental.account.dto.UpdateProfileRequest;
import com.apartment.rental.account.security.CustomUserDetails;
import com.apartment.rental.account.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;

    @GetMapping({"/profile", "/customer/profile"})
    public String viewProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Long userId = userDetails.getUser().getId();
        UpdateProfileRequest profile = userService.getProfileByUserId(userId);
        model.addAttribute("profile", profile);
        return "customer/profile";
    }

    @PostMapping({"/profile", "/customer/profile"})
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute("profile") @Valid UpdateProfileRequest request,
            BindingResult bindingResult,
            Model model) {
        // Luôn giữ lại email gốc của tài khoản để hiển thị lại nếu form có lỗi validate
        request.setEmail(userDetails.getUsername());

        if (bindingResult.hasErrors()) {
            return "customer/profile";
        }

        Long userId = userDetails.getUser().getId();
        userService.updateProfile(userId, request);

        // Cập nhật lại thông tin trong Principal hiện tại
        userDetails.getUser().setFullName(request.getFullName().trim());
        userDetails.getUser().setPhone(request.getPhone().trim());
        userDetails.getUser().setIdentityDocumentNo(request.getIdentityDocumentNo().trim());

        return "redirect:/profile?success";
    }
}