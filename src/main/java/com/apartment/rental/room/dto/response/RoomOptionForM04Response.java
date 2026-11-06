package com.apartment.rental.room.dto.response;

import java.math.BigDecimal;

public record RoomOptionForM04Response(
        Long id,
        String roomCode,
        Short floorNumber,
        Short capacity,
        BigDecimal listedMonthlyRent
) {
}