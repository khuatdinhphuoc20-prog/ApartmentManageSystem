package com.apartment.rental.billing.controller;

import com.apartment.rental.billing.dto.BillingCycleItemsForm;
import com.apartment.rental.billing.dto.BillingCyclePage;
import com.apartment.rental.billing.dto.InvoiceIssuanceResult;
import com.apartment.rental.billing.service.ManualBillingCycleService;
import com.apartment.rental.billing.service.RegularInvoiceIssuanceService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/billing")
public class AdminBillingController {

    private final ManualBillingCycleService billingService;
    private final RegularInvoiceIssuanceService invoiceIssuanceService;

    public AdminBillingController(
        ManualBillingCycleService billingService,
        RegularInvoiceIssuanceService invoiceIssuanceService
    ) {
        this.billingService = billingService;
        this.invoiceIssuanceService = invoiceIssuanceService;
    }

    @GetMapping
    public String page(@RequestParam(name = "cycleId", required = false) Long cycleId, Model model) {
        BillingCyclePage page = billingService.loadPage(cycleId);
        model.addAttribute("cycles", page.cycles());
        model.addAttribute("selectedCycle", page.selectedCycle());
        model.addAttribute("billingForm", page.form());
        model.addAttribute("canIssue", page.canIssue());
        model.addAttribute("latestAllowedDate", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        return "admin/billing";
    }

    @PostMapping("/cycles")
    public String createCycle(
        @RequestParam("collectionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate collectionDate,
        RedirectAttributes redirectAttributes
    ) {
        try {
            Long cycleId = billingService.createOrOpenCycle(collectionDate);
            redirectAttributes.addFlashAttribute("successMessage", "Đã mở kỳ thu. Dữ liệu đã nhập trước đó được giữ nguyên.");
            return "redirect:/admin/billing?cycleId=" + cycleId;
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/billing";
        }
    }

    @PostMapping("/cycles/{cycleId}/items")
    public String saveUtilityAmounts(
        @PathVariable Long cycleId,
        @ModelAttribute("billingForm") BillingCycleItemsForm form,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            billingService.saveUtilityAmounts(cycleId, form, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu tiền điện nước. Ô trống vẫn được xem là chưa nhập; số 0 là khoản đã nhập bằng 0.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/billing?cycleId=" + cycleId;
    }

    @PostMapping("/cycles/{cycleId}/issue")
    public String issueInvoices(
        @PathVariable Long cycleId,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            InvoiceIssuanceResult result = invoiceIssuanceService.issueCycle(cycleId, authentication.getName());
            if (result.alreadyCompleted()) {
                redirectAttributes.addFlashAttribute("successMessage", "Kỳ thu đã hoàn tất trước đó; không tạo Invoice trùng.");
            } else {
                redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Hoàn tất kỳ thu: đã phát hành " + result.invoicesIssued()
                        + " Invoice, ghi nhận " + result.noChargeItems() + " nghĩa vụ NO_CHARGE."
                );
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/billing?cycleId=" + cycleId;
    }
}
