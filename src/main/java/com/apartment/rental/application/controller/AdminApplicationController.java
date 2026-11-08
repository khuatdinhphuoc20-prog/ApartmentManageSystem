package com.apartment.rental.application.controller;

import com.apartment.rental.account.security.CustomUserDetails;
import com.apartment.rental.application.dto.RentalApplicationFilterRequest;
import com.apartment.rental.application.dto.RentalApplicationRejectRequest;
import com.apartment.rental.application.dto.RentalApplicationResponse;
import com.apartment.rental.application.services.RentalApplicationService;
import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.service.RoomService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    //private final EntityManager entityManager;
    private final RoomService roomService;

    @GetMapping
    public String listApplications(@ModelAttribute("filter") RentalApplicationFilterRequest filter, Model model) {
        Page<RentalApplicationResponse> pageResult = applicationService.getApplicationsForAdmin(filter);

        List<RoomResponse> rooms = roomService.getAllRoomsForAdmin(null, null);
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("filter", filter);
        model.addAttribute("rooms", rooms);
        return "admin/applications";
    }

    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        Long adminId = userDetails.getUser().getId();
        applicationService.approveApplication(id, adminId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt đơn thuê #" + id + " và tạo Hợp đồng thành công!");
        return "redirect:/admin/applications";
    }
    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable Long id,
            @ModelAttribute RentalApplicationRejectRequest rejectRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        Long adminId = userDetails.getUser().getId();
        applicationService.rejectApplication(id, rejectRequest, adminId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn thuê #" + id + ".");
        return "redirect:/admin/applications";
    }
}
