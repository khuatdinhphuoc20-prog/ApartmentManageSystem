package com.apartment.rental.application.dto;

import com.apartment.rental.application.entity.RentalApplicationStatus;
import lombok.*;

@Getter
@Setter
public class RentalApplicationFilterRequest {
    private RentalApplicationStatus status; // null nghĩa là lọc Tất cả
    private Long roomId;                    // null nghĩa là lọc Tất cả phòng
    private int page = 0;                   // Trang hiện tại (mặc định trang đầu)
    private int size = 10;
}
