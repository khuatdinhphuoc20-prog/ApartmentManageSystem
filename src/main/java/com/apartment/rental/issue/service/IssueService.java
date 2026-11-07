package com.apartment.rental.issue.service;

import com.apartment.rental.issue.dto.request.CreateIssueRequest;
import com.apartment.rental.issue.dto.response.ActiveContractOptionResponse;
import com.apartment.rental.issue.dto.response.IssueResponse;
import java.util.List;

public interface IssueService {

    List<ActiveContractOptionResponse> getActiveContractsForTenant(Long tenantId);

    Long createIssue(Long reporterId, CreateIssueRequest request);

    List<IssueResponse> getMyIssues(Long reporterId);

    IssueResponse getMyIssueDetail(Long reporterId, Long issueId);

    long countUnresolvedIssues();
}