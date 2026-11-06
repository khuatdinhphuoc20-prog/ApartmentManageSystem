package com.apartment.rental.room.mapper;

import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.dto.response.RoomOptionForM04Response;
import com.apartment.rental.room.entity.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapperForM04 {

    public RoomCapacityForM04Response toCapacityResponse(Room room) {
        return new RoomCapacityForM04Response(
                room.getId(),
                room.getCapacity()
        );
    }
    public RoomOptionForM04Response toM04OptionResponse(Room room) {
        return new RoomOptionForM04Response(
                room.getId(),
                room.getRoomCode(),
                room.getFloorNumber(),
                room.getCapacity(),
                room.getListedMonthlyRent()
        );
    }
}