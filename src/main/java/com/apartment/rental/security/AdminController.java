package com.apartment.rental.security;

import com.apartment.rental.billing.entity.BillingCycleStatus;
import com.apartment.rental.billing.repository.BillingCycleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final BillingCycleRepository billingCycleRepository;

    public AdminController(BillingCycleRepository billingCycleRepository) {
        this.billingCycleRepository = billingCycleRepository;
    }

    @GetMapping("/admin")
    public String adminHome(Authentication authentication, Model model) {
        return billingCycleRepository.findFirstByStatusOrderByCollectionDateAsc(BillingCycleStatus.PENDING_INPUT)
            .map(cycle -> "redirect:/admin/billing?cycleId=" + cycle.getId())
            .orElseGet(() -> adminHomePage(authentication, model));
    }

    private String adminHomePage(Authentication authentication, Model model) {
        model.addAttribute("email", authentication.getName());
        model.addAttribute("authorities", authentication.getAuthorities());
        return "admin/home";
    }
}
