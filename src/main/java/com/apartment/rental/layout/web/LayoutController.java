package com.apartment.rental.layout.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class LayoutController {

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        if (request.isUserInRole("ADMIN")) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("rooms", List.of(
                new SampleRoom("101", 1, 24, 2, "3.200.000"),
                new SampleRoom("202", 2, 32, 3, "4.100.000"),
                new SampleRoom("301", 3, 28, 2, "3.600.000")));
        return "customer/home";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(HttpServletRequest request) {
        if (!request.isUserInRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return "admin/dashboard";
    }

    // View-only fixtures until M02 supplies the public room query.
    public record SampleRoom(String code, int floor, int area, int capacity, String monthlyPrice) {}
}
