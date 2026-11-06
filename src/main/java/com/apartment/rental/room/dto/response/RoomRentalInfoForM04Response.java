package com.apartment.rental.room.dto.response;

import com.apartment.rental.room.entity.RoomStatus;

import java.math.BigDecimal;

public record RoomRentalInfoForM04Response(
        Long roomId,
        RoomStatus status,
        BigDecimal listedMonthlyRent
) {
}