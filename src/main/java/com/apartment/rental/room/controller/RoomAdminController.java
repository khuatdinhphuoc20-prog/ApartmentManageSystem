package com.apartment.rental.room.controller;

import com.apartment.rental.room.dto.request.RoomRequest;
import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.entity.RoomStatus;
import com.apartment.rental.room.service.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/rooms")
public class RoomAdminController {

    RoomService roomService;

    // 1. DANH SÁCH TOÀN BỘ PHÒNG
    @GetMapping
    public String listRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoomStatus status,
            Model model) {

        List<RoomResponse> rooms = roomService.getAllRoomsForAdmin(keyword, status);
        model.addAttribute("rooms", rooms);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", RoomStatus.values());

        return "admin/rooms/list";
    }


    // 2. NHÓM THÊM MỚI PHÒNG
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("roomRequest", new RoomRequest());
        return "admin/rooms/create";
    }

    @PostMapping("/create")
    public String createRoom(
            @Valid @ModelAttribute("roomRequest") RoomRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "admin/rooms/create";
        }

        try {
            roomService.createRoom(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm phòng mới thành công!");
            return "redirect:/admin/rooms";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/rooms/create";
        }
    }

    // 3. NHÓM CHỈNH SỬA PHÒNG
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            RoomResponse room = roomService.getRoomById(id);
            model.addAttribute("room", room);
            return "admin/rooms/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/rooms";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateRoom(
            @PathVariable Long id,
            @Valid @ModelAttribute("roomRequest") RoomRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            try {
                RoomResponse room = roomService.getRoomById(id);
                room.setRoomCode(request.getRoomCode());
                room.setFloorNumber(request.getFloorNumber());
                room.setCapacity(request.getCapacity());
                room.setListedMonthlyRent(request.getListedMonthlyRent());
                room.setAreaM2(request.getAreaM2());
                room.setCoverImagePath(request.getCoverImagePath());
                room.setDescription(request.getDescription());
                model.addAttribute("room", room);
            } catch (Exception ignored) {}
            return "admin/rooms/edit";
        }

        try {
            roomService.updateRoom(request, id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phòng thành công!");
            return "redirect:/admin/rooms";
        } catch (Exception e) {
            try {
                RoomResponse room = roomService.getRoomById(id);
                room.setRoomCode(request.getRoomCode());
                room.setFloorNumber(request.getFloorNumber());
                room.setCapacity(request.getCapacity());
                room.setListedMonthlyRent(request.getListedMonthlyRent());
                room.setAreaM2(request.getAreaM2());
                room.setCoverImagePath(request.getCoverImagePath());
                room.setDescription(request.getDescription());
                model.addAttribute("room", room);
            } catch (Exception ignored) {}
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/rooms/edit";
        }
    }

    // 4. ĐÓNG / MỞ PHÒNG TRỐNG
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.toggleRoomStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/rooms";
    }
}
