package com.apartment.rental.issue.service;

import com.apartment.rental.issue.dto.request.CreateIssueRequest;
import com.apartment.rental.issue.dto.request.UpdateIssueStatusRequest;
import com.apartment.rental.issue.dto.response.ActiveContractOptionResponse;
import com.apartment.rental.issue.dto.response.AdminIssueResponse;
import com.apartment.rental.issue.dto.response.IssueResponse;
import com.apartment.rental.issue.entity.IssueStatus;
import java.util.List;

public interface IssueService {

    // UC36 & UC37 (Tenant)
    List<ActiveContractOptionResponse> getActiveContractsForTenant(Long tenantId);

    Long createIssue(Long reporterId, CreateIssueRequest request);

    List<IssueResponse> getMyIssues(Long reporterId);

    IssueResponse getMyIssueDetail(Long reporterId, Long issueId);

    // UC38 (Admin)
    List<AdminIssueResponse> getAllIssuesForAdmin(IssueStatus statusFilter);

    AdminIssueResponse getIssueDetailForAdmin(Long issueId);

    boolean updateIssueByAdmin(Long issueId, Long adminUserId, UpdateIssueStatusRequest request);

    // Dashboard Query cho M06 (D21)
    long countUnresolvedIssues();
}