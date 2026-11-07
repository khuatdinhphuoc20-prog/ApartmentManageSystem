package com.apartment.rental.issue.event;

import com.apartment.rental.issue.entity.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueStatusUpdatedEvent {
    private Long issueId;
    private Long recipientUserId;
    private IssueStatus newStatus;
    private String eventKey;
    private String title;
    private String body;
}