package com.apartment.rental.contract.controller;

import com.apartment.rental.contract.dto.request.ContractCoOccupantRequest;
import com.apartment.rental.contract.dto.request.DirectContractRequest;
import com.apartment.rental.contract.dto.response.ContractDetailResponse;
import com.apartment.rental.contract.entity.ContractEndReason;
import com.apartment.rental.contract.entity.ContractStatus;
import com.apartment.rental.contract.exception.BusinessException;
import com.apartment.rental.contract.exception.ResourceNotFoundException;
import com.apartment.rental.contract.service.ContractCoOccupantService;
import com.apartment.rental.contract.service.ContractService;
import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.service.RoomServiceForM04;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final RoomServiceForM04 roomServiceForM04;
    private final ContractCoOccupantService coOccupantService;
    @GetMapping
    public String showContractList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContractStatus status,
            Model model
    ) {
        model.addAttribute(
                "contracts",
                contractService.getAllContracts(keyword, status)
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("statusFilter", status);
        model.addAttribute("statuses", ContractStatus.values());

        return "admin/contracts/list";
    }
    @GetMapping("/create")
    public String showCreateForm(Model model) {

        DirectContractRequest request = new DirectContractRequest();

        model.addAttribute("directContractRequest", request);

        loadCreateFormData(model, request);

        return "admin/contracts/create";
    }
    @PostMapping("/create/load-capacity")
    public String loadRoomCapacity(
            @ModelAttribute("directContractRequest")
            DirectContractRequest request,
            Model model
    ) {
        // Đổi phòng thì xóa danh sách người ở cùng cũ
        request.setCoOccupants(new ArrayList<>());

        model.addAttribute("directContractRequest", request);

        loadCreateFormData(model, request);

        return "admin/contracts/create";
    }
    @PostMapping("/direct")
    public String createDirectContract(
            @Valid @ModelAttribute("directContractRequest")
            DirectContractRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            loadCreateFormData(model,request);
            return "admin/contracts/create";
        }

        try {
            // TODO: Lấy adminId từ tài khoản đang đăng nhập.
            Long adminId =6L;

            ContractDetailResponse contract =
                    contractService.createDirectContract(request, adminId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tạo hợp đồng thành công!"
            );

            return "redirect:/admin/contracts/" + contract.id();

        } catch (BusinessException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            loadCreateFormData(model,request);
            return "admin/contracts/create";
        }
    }

    @GetMapping("/{id}")
    public String showContractDetail(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            Model model) {

        model.addAttribute(
                "contract",
                contractService.getContractDetail(id)
        );

        return "admin/contracts/detail";
    }
    @GetMapping("/{id}/edit")
    public String showEditContractForm(
            @PathVariable Long id,
            Model model
    ) {
        ContractDetailResponse contract = contractService.getContractDetail(id);

        if (contract.status() == ContractStatus.ENDED
                || contract.status() == ContractStatus.CANCELLED) {
            return "redirect:/admin/contracts/" + id;
        }

        model.addAttribute("contract", contract);
        model.addAttribute(
                "coOccupants",
                coOccupantService.getCoOccupants(id)
        );

        return "admin/contracts/edit";
    }
    @PostMapping("/{contractId}/co-occupants")
    public String addCoOccupant(
            @PathVariable Long contractId,
            @Valid @ModelAttribute("coOccupantRequest")
            ContractCoOccupantRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Thông tin người ở cùng không hợp lệ."
            );
            return "redirect:/admin/contracts/" + contractId + "/edit";
        }

        try {
            coOccupantService.addCoOccupant(contractId, request);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Thêm người ở cùng thành công!"
            );
        } catch (BusinessException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/contracts/" + contractId + "/edit";
    }

    @PostMapping("/{contractId}/co-occupants/{coOccupantId}")
    public String updateCoOccupant(
            @PathVariable Long contractId,
            @PathVariable Long coOccupantId,
            @Valid @ModelAttribute("coOccupantRequest")
            ContractCoOccupantRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Thông tin người ở cùng không hợp lệ."
            );
            return "redirect:/admin/contracts/" + contractId + "/edit";
        }

        try {
            coOccupantService.updateCoOccupant(
                    contractId,
                    coOccupantId,
                    request
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật người ở cùng thành công!"
            );
        } catch (BusinessException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/contracts/" + contractId + "/edit";
    }

    @PostMapping("/{contractId}/co-occupants/{coOccupantId}/delete")
    public String deleteCoOccupant(
            @PathVariable Long contractId,
            @PathVariable Long coOccupantId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            coOccupantService.deleteCoOccupant(contractId, coOccupantId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Xóa người ở cùng thành công!"
            );
        } catch (BusinessException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/contracts/" + contractId + "/edit";
    }

    @PostMapping("/{id}/cancel")
    public String cancelContract(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Long adminId = 6L; // Tạm thời, sau này lấy từ tài khoản đăng nhập

            contractService.cancelScheduledContract(id, adminId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Hủy hợp đồng thành công!"
            );

        } catch (BusinessException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/contracts/" + id;
    }
    private void loadCreateFormData(
            Model model,
            DirectContractRequest request
    ) {
        model.addAttribute(
                "tenants",
                contractService.getAvailableTenants()
        );

        model.addAttribute(
                "rooms",
                contractService.getAvailableRooms()
        );

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Ho_Chi_Minh")
        );

        model.addAttribute("minStartDate", today);
        model.addAttribute("maxStartDate", today.plusDays(10));
        model.addAttribute("minEndYear", today.getYear());

        int coOccupantSlots = 0;

        if (request.getRoomId() != null) {
            try {
                RoomCapacityForM04Response capacity =
                        roomServiceForM04.getRoomCapacityForM04(
                                request.getRoomId()
                        );

                if (capacity.capacity() != null
                        && capacity.capacity() > 0) {
                    coOccupantSlots = capacity.capacity() - 1;
                }

            } catch (Exception e) {
                model.addAttribute(
                        "errorMessage",
                        "Không thể lấy sức chứa của phòng đã chọn."
                );
            }
        }

        model.addAttribute("coOccupantSlots", coOccupantSlots);
    }
}