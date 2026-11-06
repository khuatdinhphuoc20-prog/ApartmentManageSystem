package com.apartment.rental.room.service;

import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.dto.response.RoomOptionForM04Response;
import com.apartment.rental.room.dto.response.RoomRentalInfoForM04Response;

import java.util.List;

public interface RoomServiceForM04 {

    RoomCapacityForM04Response getRoomCapacityForM04(Long roomId);
    void releaseRoomForM04(Long roomId);
    RoomRentalInfoForM04Response getRoomRentalInfoForM04(Long roomId);
    void reserveRoomForM04(Long roomId);
    List<RoomOptionForM04Response> getAvailableRooms();
}