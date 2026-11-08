package com.apartment.rental.room.port;

import com.apartment.rental.room.entity.RoomStatus;

public interface RoomOccupancyCommandPort {

    void reserveRoom(Long roomId);

    void releaseRoom(Long roomId, RoomStatus targetStatus);
}
