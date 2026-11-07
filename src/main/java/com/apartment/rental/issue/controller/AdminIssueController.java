package com.apartment.rental.issue.controller;

import com.apartment.rental.account.security.CustomUserDetails;
import com.apartment.rental.issue.dto.request.UpdateIssueStatusRequest;
import com.apartment.rental.issue.dto.response.AdminIssueResponse;
import com.apartment.rental.issue.entity.IssueStatus;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/issues")
@RequiredArgsConstructor
@Slf4j
public class AdminIssueController {

    private final IssueService issueService;

    @GetMapping
    public String listIssues(
            @RequestParam(name = "status", required = false) IssueStatus status,
            Model model) {
        List<AdminIssueResponse> issues = issueService.getAllIssuesForAdmin(status);
        model.addAttribute("issues", issues);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("unresolvedCount", issueService.countUnresolvedIssues());
        return "admin/issue/list";
    }

    @GetMapping("/{id}")
    public String viewIssueDetail(@PathVariable("id") Long issueId, Model model) {
        AdminIssueResponse issue = issueService.getIssueDetailForAdmin(issueId);

        UpdateIssueStatusRequest updateForm = new UpdateIssueStatusRequest();

        if (issue.getStatus() == IssueStatus.OPEN) {
            updateForm.setStatus(IssueStatus.IN_PROGRESS);
        } else if (issue.getStatus() == IssueStatus.IN_PROGRESS) {
            updateForm.setStatus(IssueStatus.RESOLVED);
        } else {
            updateForm.setStatus(IssueStatus.RESOLVED);
        }
        updateForm.setAdminResponse(issue.getAdminResponse());

        model.addAttribute("issue", issue);
        model.addAttribute("updateForm", updateForm);
        return "admin/issue/detail";
    }

    @PostMapping("/{id}")
    public String updateIssue(
            @PathVariable("id") Long issueId,
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @ModelAttribute("updateForm") @Valid UpdateIssueStatusRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("issue", issueService.getIssueDetailForAdmin(issueId));
            return "admin/issue/detail";
        }

        Long adminId = adminDetails.getUser().getId();
        issueService.updateIssueByAdmin(issueId, adminId, request);

        return "redirect:/admin/issues/" + issueId + "?updated";
    }
}