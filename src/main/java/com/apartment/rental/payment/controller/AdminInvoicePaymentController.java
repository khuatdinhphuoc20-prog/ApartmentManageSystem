package com.apartment.rental.payment.controller;

import com.apartment.rental.billing.service.AdminInvoicePaymentService;
import com.apartment.rental.invoice.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminInvoicePaymentController {

    private final AdminInvoicePaymentService paymentService;

    public AdminInvoicePaymentController(AdminInvoicePaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/admin/payment/reconciliation")
    public String paymentPage(Authentication authentication, Model model) {
        model.addAttribute("invoices", paymentService.findOutstandingInvoices(authentication.getName()));
        model.addAttribute("paymentMethods", new PaymentMethod[] {PaymentMethod.CASH, PaymentMethod.BANK_TRANSFER});
        return "admin/payment-record";
    }

    @PostMapping("/admin/payment/reconciliation/{invoiceId}")
    public String recordPayment(
        @PathVariable Long invoiceId,
        Authentication authentication,
        @RequestParam BigDecimal receivedAmount,
        @RequestParam PaymentMethod paymentMethod,
        @RequestParam(required = false) String paymentReference,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paidAt,
        @RequestParam(defaultValue = "false") boolean receivedInFull,
        RedirectAttributes redirectAttributes
    ) {
        try {
            if (!receivedInFull) {
                throw new IllegalArgumentException("Chỉ xác nhận khi bạn đã nhận đủ số tiền trên hóa đơn.");
            }
            paymentService.recordReceivedPayment(
                invoiceId,
                authentication.getName(),
                receivedAmount,
                paymentMethod,
                paymentReference,
                paidAt
            );
            redirectAttributes.addFlashAttribute("paymentMessage", "Đã ghi nhận thanh toán. Hóa đơn đã được cập nhật.");
            redirectAttributes.addFlashAttribute("paymentError", false);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("paymentMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("paymentError", true);
        }
        return "redirect:/admin/payment/reconciliation";
    }
}
