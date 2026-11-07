package com.apartment.rental.issue.dto.request;

import com.apartment.rental.issue.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueStatusRequest {

    @NotNull(message = "Vui lòng chọn trạng thái xử lý!")
    private IssueStatus status;

    @Size(max = 4000, message = "Nội dung phản hồi không được vượt quá 4000 ký tự!")
    private String adminResponse;
}