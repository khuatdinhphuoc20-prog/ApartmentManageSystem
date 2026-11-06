package com.apartment.rental.account.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpServletRequest request) {
        if (request.isUserInRole("ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        if (request.isUserInRole("USER")) {
            return "redirect:/customer/applications";
        }
        return "redirect:/rooms";
    }
}
