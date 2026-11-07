package com.apartment.rental.issue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIssueRequest {

    @NotNull(message = "Vui lòng chọn phòng/hợp đồng đang thuê!")
    private Long contractId;

    @NotBlank(message = "Tiêu đề phản ánh không được để trống!")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự!")
    private String title;

    @NotBlank(message = "Nội dung chi tiết không được để trống!")
    @Size(max = 4000, message = "Nội dung mô tả không được vượt quá 4000 ký tự!")
    private String description;
}