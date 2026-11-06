package com.apartment.rental.billing.controller;

import com.apartment.rental.billing.dto.BillingPeriodRowDto;
import com.apartment.rental.invoice.service.RegularInvoiceIssuanceService;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/billing")
public class AdminBillingController {

    private final RegularInvoiceIssuanceService invoiceIssuanceService;

    public AdminBillingController(RegularInvoiceIssuanceService invoiceIssuanceService) {
        this.invoiceIssuanceService = invoiceIssuanceService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("duePeriods", invoiceIssuanceService.loadDuePeriods());
        return "admin/billing";
    }

    @PostMapping("/contracts/{contractId}/issue")
    public String issueInvoices(
        @PathVariable Long contractId,
        @RequestParam("electricityAmount") BigDecimal electricityAmount,
        @RequestParam("waterAmount") BigDecimal waterAmount,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Optional<Long> invoiceId = invoiceIssuanceService.issuePeriod(
                contractId,
                electricityAmount,
                waterAmount,
                authentication.getName()
            );
            String message = invoiceId
                .map(id -> "Đã phát hành hóa đơn #" + id + ".")
                .orElse("Đã chốt kỳ này: tổng tiền bằng 0 nên không tạo hóa đơn.");
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException | IllegalStateException | DataIntegrityViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/billing";
    }
}
