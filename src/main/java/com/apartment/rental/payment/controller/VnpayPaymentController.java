package com.apartment.rental.payment.controller;

import com.apartment.rental.payment.service.VnpayPaymentService;
import com.apartment.rental.payment.service.VnpayPaymentService.ReturnResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class VnpayPaymentController {

    private final VnpayPaymentService paymentService;

    public VnpayPaymentController(VnpayPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment/invoices/{invoiceId}/vnpay")
    public RedirectView startPayment(
        @PathVariable Long invoiceId,
        Authentication authentication,
        HttpServletRequest request
    ) {
        return new RedirectView(paymentService.createPaymentUrl(invoiceId, authentication.getName(), request));
    }

    @GetMapping("/payment/vnpay/return")
    public RedirectView vnpayReturn(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            ReturnResult result = paymentService.processReturn(request);
            redirectAttributes.addFlashAttribute("paymentMessage", result.message());
            redirectAttributes.addFlashAttribute("paymentError", !result.success());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                "paymentMessage",
                "Thông tin giao dịch chưa hợp lệ nên hóa đơn chưa được cập nhật. Nếu bạn đã thanh toán, vui lòng liên hệ quản lý.");
            redirectAttributes.addFlashAttribute("paymentError", true);
        }
        return new RedirectView("/customer/invoices", true);
    }
}
