package com.apartment.rental.layout.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class LayoutController {

    @GetMapping("/")
    public String home(HttpServletRequest request) {
        if (request.isUserInRole("ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/rooms";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(HttpServletRequest request) {
        if (!request.isUserInRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return "admin/dashboard";
    }
}
