package com.apartment.rental.room.dto.response;

import com.apartment.rental.room.entity.RoomStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
public class RoomResponse {

    Long id;
    String roomCode;
    String description;
    BigDecimal areaM2;
    Short floorNumber;
    String coverImagePath;
    Short capacity;
    BigDecimal listedMonthlyRent;
    RoomStatus status;
    Instant createdAt;
    Instant updatedAt;
}
