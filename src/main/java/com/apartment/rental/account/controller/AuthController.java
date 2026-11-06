package com.apartment.rental.account.controller;

import com.apartment.rental.account.dto.LoginRequest;
import com.apartment.rental.account.dto.RegisterRequest;
import com.apartment.rental.account.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @GetMapping({"/register", "/auths/register"})
    public String viewRegisterForm(Model model) {
        model.addAttribute("user", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping({"/register", "/auths/register"})
    public String processRegister(
            @ModelAttribute("user") @Valid RegisterRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        authService.register(request);

        return "redirect:/login?success";
    }

    @GetMapping({"/login", "/auths/login"})
    public String viewLoginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }
}