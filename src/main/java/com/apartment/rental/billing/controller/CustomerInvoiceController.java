package com.apartment.rental.billing.controller;

import com.apartment.rental.billing.dto.CustomerInvoicePageDto;
import com.apartment.rental.billing.service.CustomerInvoiceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerInvoiceController {

    private final CustomerInvoiceService customerInvoiceService;

    public CustomerInvoiceController(CustomerInvoiceService customerInvoiceService) {
        this.customerInvoiceService = customerInvoiceService;
    }

    @GetMapping("/customer/invoices")
    public String myInvoices(Authentication authentication, Model model) {
        CustomerInvoicePageDto page = customerInvoiceService.getMyInvoices(authentication.getName());
        model.addAttribute("page", page);
        return "customer/invoices";
    }
}
