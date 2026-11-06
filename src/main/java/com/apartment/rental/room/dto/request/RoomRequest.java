package com.apartment.rental.room.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomRequest {

    @NotBlank(message = "Mã phòng không được để trống")
    String roomCode;

    String description;

    @Positive(message = "Diện tích nếu có phải lớn hơn 0")
    BigDecimal areaM2;

    @NotNull(message = "Tầng không được để trống")
    @Min(value = 1, message = "Tầng phải lớn hơn 0")
    Short floorNumber;

    @NotBlank(message = "Ảnh đại diện không được để trống")
    String coverImagePath;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    Short capacity;

    @NotNull(message = "Giá thuê niêm yết không được để trống")
    @Positive(message = "Giá thuê niêm yết phải lớn hơn 0")
    BigDecimal listedMonthlyRent;
}
