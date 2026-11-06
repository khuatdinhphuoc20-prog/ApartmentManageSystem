package com.apartment.rental.application.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class RentalApplicationCreateRequest {

    @NotNull(message = "Phòng thuê không được để trống")
    private Long roomId;

    @NotNull(message = "Ngày bắt đầu thuê không được để trống")
    @DateTimeFormat(pattern = "dd/MM/yyyy", fallbackPatterns = {"yyyy-MM-dd"})
    private LocalDate requestedStartDate;

    private Integer durationMonths;
    private Integer endMonth;
    private Integer endYear;
    @DateTimeFormat(pattern = "dd/MM/yyyy", fallbackPatterns = {"yyyy-MM-dd"})
    private LocalDate requestedEndDate;
}
