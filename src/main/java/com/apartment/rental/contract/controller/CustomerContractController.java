package com.apartment.rental.contract.controller;

import com.apartment.rental.account.security.CustomUserDetails;
import com.apartment.rental.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customer/contracts")
public class CustomerContractController {

    private final ContractService contractService;

    @GetMapping
    public String myContracts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        model.addAttribute("contracts", contractService.getTenantContracts(userDetails.getUser().getId()));
        return "customer/contracts";
    }

    @GetMapping("/{contractId}")
    public String contractDetail(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        model.addAttribute(
                "contract",
                contractService.getTenantContractDetail(contractId, userDetails.getUser().getId())
        );
        return "customer/contract-detail";
    }
}
