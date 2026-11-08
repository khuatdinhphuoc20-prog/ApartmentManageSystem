package com.apartment.rental.issue.controller;

import com.apartment.rental.account.security.CustomUserDetails;
import com.apartment.rental.issue.dto.request.CreateIssueRequest;
import com.apartment.rental.issue.dto.response.ActiveContractOptionResponse;
import com.apartment.rental.issue.dto.response.IssueResponse;
import com.apartment.rental.issue.service.IssueService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class IssueController {

    private final IssueService issueService;

    @GetMapping({"/issues", "/customer/issues"})
    public String listMyIssues(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Long userId = userDetails.getUser().getId();
        List<IssueResponse> issues = issueService.getMyIssues(userId);
        List<ActiveContractOptionResponse> activeContracts = issueService.getActiveContractsForTenant(userId);

        model.addAttribute("issues", issues);
        model.addAttribute("canCreateIssue", !activeContracts.isEmpty());
        return "customer/issue/list";
    }

    @GetMapping({"/issues/new", "/customer/issues/new"})
    public String viewCreateIssueForm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Long userId = userDetails.getUser().getId();
        List<ActiveContractOptionResponse> activeContracts = issueService.getActiveContractsForTenant(userId);

        CreateIssueRequest issueForm = new CreateIssueRequest();
        if (activeContracts.size() == 1) {
            issueForm.setContractId(activeContracts.get(0).getContractId());
        }

        model.addAttribute("activeContracts", activeContracts);
        model.addAttribute("issueForm", issueForm);
        return "customer/issue/create";
    }

    @PostMapping({"/issues", "/customer/issues"})
    public String processCreateIssue(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute("issueForm") @Valid CreateIssueRequest request,
            BindingResult bindingResult,
            Model model) {
        Long userId = userDetails.getUser().getId();

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeContracts", issueService.getActiveContractsForTenant(userId));
            return "customer/issue/create";
        }

        Long issueId = issueService.createIssue(userId, request);
        return "redirect:/issues/" + issueId + "?created";
    }

    @GetMapping({"/issues/{id}", "/customer/issues/{id}"})
    public String viewIssueDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long issueId,
            Model model) {
        Long userId = userDetails.getUser().getId();
        IssueResponse issue = issueService.getMyIssueDetail(userId, issueId);
        model.addAttribute("issue", issue);
        return "customer/issue/detail";
    }
}