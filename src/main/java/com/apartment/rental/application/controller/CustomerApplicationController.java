package com.apartment.rental.application.controller;

import com.apartment.rental.account.security.CustomUserDetails;
import com.apartment.rental.application.dto.RentalApplicationCreateRequest;
import com.apartment.rental.application.dto.RentalApplicationResponse;
import com.apartment.rental.application.services.RentalApplicationService;
import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.entity.Room;
import com.apartment.rental.room.entity.RoomStatus;
import com.apartment.rental.room.port.RoomQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    //private final EntityManager entityManager;
    private final RoomQueryPort roomQueryPort;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @GetMapping
    public String myApplications(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long currentUserId = userDetails.getUser().getId();
        List<RentalApplicationResponse> applications = applicationService.getMyApplications(currentUserId);
        model.addAttribute("applications", applications);
        return "customer/applications";
    }

    @GetMapping("/apply/{roomId}")
    public String showApplyForm(@PathVariable Long roomId, Model model) {
        RoomResponse room = roomQueryPort.getRoomDetail(roomId);

        if (room == null || room.getStatus() != RoomStatus.AVAILABLE) {
            return "redirect:/rooms?error=room_not_available";
        }

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        RentalApplicationCreateRequest request = new RentalApplicationCreateRequest();
        request.setRoomId(roomId);
        request.setRequestedStartDate(today);
        request.setDurationMonths(1);

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
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            RoomResponse room = roomQueryPort.getRoomDetail(request.getRoomId());
            model.addAttribute("room", room);
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            model.addAttribute("today", today);
            model.addAttribute("maxStartDate", today.plusDays(10));
            model.addAttribute("currentYear", today.getYear());
            return "customer/apply";
        }
        Long currentUserId = userDetails.getUser().getId();
        applicationService.createApplication(request, currentUserId);
        redirectAttributes.addFlashAttribute("successMessage", "Gửi đơn đăng ký thuê phòng thành công!");
        return "redirect:/customer/applications";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        Long currentUserId = userDetails.getUser().getId();
        applicationService.withdrawApplication(id, currentUserId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã rút đơn đăng ký thuê thành công!");
        return "redirect:/customer/applications";
    }
}
