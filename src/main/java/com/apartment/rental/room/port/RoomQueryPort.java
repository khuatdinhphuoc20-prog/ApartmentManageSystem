package com.apartment.rental.room.port;

import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.dto.response.RoomSummaryResponse;
import com.apartment.rental.room.entity.RoomStatus;
import java.math.BigDecimal;
import java.util.Map;

public interface RoomQueryPort {

    RoomResponse getRoomDetail(Long roomId);

    RoomSummaryResponse getRoomSummary(Long roomId);

    BigDecimal getListedMonthlyRent(Long roomId);

    Short getRoomCapacity(Long roomId);

    boolean isRoomAvailable(Long roomId);

    Map<RoomStatus, Long> countRoomsByStatus();
}
