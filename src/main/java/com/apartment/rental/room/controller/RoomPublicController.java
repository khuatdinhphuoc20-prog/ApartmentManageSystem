package com.apartment.rental.room.controller;

import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.service.RoomService;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/rooms")
public class RoomPublicController {

    RoomService roomService;

    // Xem danh sách phòng khả dụng + Tìm kiếm & Lọc
    @GetMapping
    public String listRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short floorNumber,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Short minCapacity,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            Model model) {

        List<RoomResponse> rooms;

        // Nếu có truyền bất kỳ tiêu chí lọc nào thì gọi hàm filter
        boolean hasFilter = floorNumber != null || minPrice != null || maxPrice != null
                || minCapacity != null || minArea != null || maxArea != null;

        if (hasFilter) {
            rooms = roomService.filterAvailableRooms(floorNumber, minPrice, maxPrice, minCapacity, minArea, maxArea);
        } else {
            rooms = roomService.getAllAvailableRooms(keyword);
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("keyword", keyword);
        model.addAttribute("floorNumber", floorNumber);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minCapacity", minCapacity);
        model.addAttribute("minArea", minArea);
        model.addAttribute("maxArea", maxArea);

        // Render ra giao diện customer/home
        return "customer/home";
    }

    // Xem chi tiết một phòng khả dụng
    @GetMapping("/{id}")
    public String roomDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            RoomResponse room = roomService.getRoomDetail(id);
            model.addAttribute("room", room);
            return "customer/room-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/rooms";
        }
    }
}
