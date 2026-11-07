package com.apartment.rental.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualNotificationForm {

    @NotBlank(message = "Vui lòng nhập tiêu đề thông báo.")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự.")
    private String title;

    @NotBlank(message = "Vui lòng nhập nội dung thông báo.")
    @Size(max = 4000, message = "Nội dung không được vượt quá 4.000 ký tự.")
    private String body;

    @NotEmpty(message = "Vui lòng chọn ít nhất một người nhận.")
    private List<@NotNull(message = "Người nhận không hợp lệ.")
                 @Positive(message = "Người nhận không hợp lệ.") Long> recipientIds = new ArrayList<>();
}
