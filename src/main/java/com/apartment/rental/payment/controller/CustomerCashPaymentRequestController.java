package com.apartment.rental.payment.controller;

import com.apartment.rental.billing.service.CustomerCashPaymentRequestService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerCashPaymentRequestController {

    private final CustomerCashPaymentRequestService requestService;

    public CustomerCashPaymentRequestController(CustomerCashPaymentRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/customer/invoices/{invoiceId}/cash-payment-request")
    public String requestCashPayment(
        @PathVariable Long invoiceId,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            boolean created = requestService.requestCashPayment(invoiceId, authentication.getName());
            redirectAttributes.addFlashAttribute(
                "paymentMessage",
                created
                    ? "Đã gửi yêu cầu thanh toán tiền mặt đến quản lý. Hóa đơn sẽ được cập nhật sau khi quản lý xác nhận đã nhận đủ tiền."
                    : "Bạn đã gửi yêu cầu thanh toán tiền mặt cho hóa đơn này rồi."
            );
            redirectAttributes.addFlashAttribute("paymentError", false);
        } catch (AccessDeniedException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("paymentMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("paymentError", true);
        }
        return "redirect:/customer/invoices#invoice-" + invoiceId;
    }
}
