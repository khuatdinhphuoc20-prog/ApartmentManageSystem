package com.apartment.rental.application.controller;

import com.apartment.rental.application.dto.RentalApplicationFilterRequest;
import com.apartment.rental.application.dto.RentalApplicationRejectRequest;
import com.apartment.rental.application.dto.RentalApplicationResponse;
import com.apartment.rental.application.services.RentalApplicationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/applications")
@RequiredArgsConstructor
public class AdminApplicationController {
    private final RentalApplicationService applicationService;
    private final EntityManager entityManager;

    @GetMapping
    public String listApplications(@ModelAttribute("filter") RentalApplicationFilterRequest filter, Model model) {
        Page<RentalApplicationResponse> pageResult = applicationService.getApplicationsForAdmin(filter);
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("filter", filter);
        return "admin/applications";
    }

    @PostMapping("/{id}/approve")
    public  String approve(@PathVariable long id, Principal principal, RedirectAttributes redirectAttributes) {
        Long adminId = resolveAdminId(principal);
        applicationService.approveApplication(id, adminId);
        redirectAttributes.addFlashAttribute("successMessage", "Application #" + id + " has been approved successfully!");
        return "redirect:/admin/applications";
    }

    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable Long id, @ModelAttribute RentalApplicationRejectRequest rejectRequest,
            Principal principal, RedirectAttributes redirectAttributes){
        Long adminId = resolveAdminId(principal);
        applicationService.rejectApplication(id, rejectRequest, adminId);
        redirectAttributes.addFlashAttribute("successMessage", "Application #" + id + " has been rejected.");
        return "redirect:/admin/applications";
    }

    //Hàm lấy admin
    private Long resolveAdminId(Principal principal) {
        if (principal != null) {
            try {
                List<Long> ids = entityManager.createQuery("SELECT u.id FROM User u WHERE u.email = :email", Long.class)
                        .setParameter("email", principal.getName())
                        .getResultList();
                if (!ids.isEmpty()) {
                    return ids.get(0);
                }
            } catch (Exception ignored) {}
        }
        return 1L; // Fallback Admin ID mặc định
    }
}
