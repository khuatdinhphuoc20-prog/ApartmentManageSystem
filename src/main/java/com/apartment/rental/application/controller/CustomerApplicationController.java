package com.apartment.rental.application.controller;

import com.apartment.rental.application.dto.RentalApplicationCreateRequest;
import com.apartment.rental.application.dto.RentalApplicationResponse;
import com.apartment.rental.application.services.RentalApplicationService;
import com.apartment.rental.room.entity.Room;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/customer/applications")
@RequiredArgsConstructor
public class CustomerApplicationController {

    private final RentalApplicationService applicationService;
    private final EntityManager entityManager;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @GetMapping
    public String myApplications(Principal principal, Model model) {
        Long currentUserId = resolveUserId(principal, "USER");
        List<RentalApplicationResponse> applications = applicationService.getMyApplications(currentUserId);
        model.addAttribute("applications", applications);
        return "customer/applications";
    }

    @GetMapping("/apply/{roomId}")
    public String showApplyForm(@PathVariable Long roomId, Model model) {
        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            return "redirect:/?error=room_not_found";
        }

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        RentalApplicationCreateRequest request = new RentalApplicationCreateRequest();
        request.setRoomId(roomId);
        request.setRequestedStartDate(today);
        request.setDurationMonths(1); // Mặc định gói 1 tháng

        LocalDate defaultEndDate = today.plusMonths(1).withDayOfMonth(10);
        if (!defaultEndDate.isAfter(today)) {
            defaultEndDate = defaultEndDate.plusMonths(1);
        }
        request.setEndMonth(defaultEndDate.getMonthValue());
        request.setEndYear(defaultEndDate.getYear());

        model.addAttribute("room", room);
        model.addAttribute("applyRequest", request);
        model.addAttribute("today", today);
        model.addAttribute("maxStartDate", today.plusDays(10));
        model.addAttribute("currentYear", today.getYear());
        return "customer/apply";
    }

    @PostMapping("/apply")
    public String submitApply(
            @Valid @ModelAttribute("applyRequest") RentalApplicationCreateRequest request,
            BindingResult bindingResult, Principal principal,
            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Room room = entityManager.find(Room.class, request.getRoomId());
            model.addAttribute("room", room);
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            model.addAttribute("today", today);
            model.addAttribute("maxStartDate", today.plusDays(10));
            return "customer/apply";
        }
        Long currentUserId = resolveUserId(principal, "USER");
        applicationService.createApplication(request, currentUserId);
        redirectAttributes.addFlashAttribute("successMessage", "Gửi đơn đăng ký thuê phòng thành công!");
        return "redirect:/customer/applications";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        Long currentUserId = resolveUserId(principal, "USER");
        applicationService.withdrawApplication(id, currentUserId);
        redirectAttributes.addFlashAttribute("successMessage", "Huỷ đơn thành công!");
        return "redirect:/customer/applications";
    }

    //Xoá sau khi có hàm lấy user
    private Long resolveUserId(Principal principal, String role) {
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
        return "ADMIN".equals(role) ? 1L : 2L;
    }
}
