package com.apartment.rental.issue.dto.response;

import com.apartment.rental.issue.entity.IssueStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminIssueResponse {
    private Long id;
    private Long contractId;
    private String contractStatus;
    private String roomCode;
    private Number floorNumber;
    private Long reporterId;
    private String reporterFullName;
    private String reporterEmail;
    private String reporterPhone;
    private String title;
    private String description;
    private IssueStatus status;
    private String handledByAdminName;
    private String adminResponse;
    private Instant createdAt;
    private Instant inProgressAt;
    private Instant resolvedAt;
}