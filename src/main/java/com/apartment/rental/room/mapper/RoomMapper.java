package com.apartment.rental.room.mapper;

import com.apartment.rental.room.dto.request.RoomRequest;
import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.dto.response.RoomOptionForM04Response;
import com.apartment.rental.room.dto.response.RoomResponse;
import com.apartment.rental.room.dto.response.RoomRentalInfoForM04Response;
import com.apartment.rental.room.dto.response.RoomSummaryResponse;
import com.apartment.rental.room.entity.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

    public Room toRoom(RoomRequest request) {
        if (request == null) {
            return null;
        }
        Room room = new Room();
        room.setRoomCode(request.getRoomCode().trim());
        room.setDescription(request.getDescription());
        room.setAreaM2(request.getAreaM2());
        room.setFloorNumber(request.getFloorNumber());
        room.setCoverImagePath(request.getCoverImagePath().trim());
        room.setCapacity(request.getCapacity());
        room.setListedMonthlyRent(request.getListedMonthlyRent());
        return room;
    }

    public RoomResponse toRoomResponse(Room room) {
        if (room == null) {
            return null;
        }
        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .description(room.getDescription())
                .areaM2(room.getAreaM2())
                .floorNumber(room.getFloorNumber())
                .coverImagePath(room.getCoverImagePath())
                .capacity(room.getCapacity())
                .listedMonthlyRent(room.getListedMonthlyRent())
                .status(room.getStatus())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    public RoomSummaryResponse toRoomSummaryResponse(Room room) {
        if (room == null) {
            return null;
        }
        return RoomSummaryResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .floorNumber(room.getFloorNumber())
                .listedMonthlyRent(room.getListedMonthlyRent())
                .build();
    }

    public RoomCapacityForM04Response toCapacityResponse(Room room) {
        return new RoomCapacityForM04Response(room.getId(), room.getCapacity());
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

    public RoomRentalInfoForM04Response toRentalInfoForM04Response(Room room) {
        return new RoomRentalInfoForM04Response(
                room.getId(),
                room.getStatus(),
                room.getListedMonthlyRent()
        );
    }

    public void updateRoomFromRequest(Room room, RoomRequest request) {
        if (room == null || request == null) {
            return;
        }
        room.setRoomCode(request.getRoomCode().trim());
        room.setDescription(request.getDescription());
        room.setAreaM2(request.getAreaM2());
        room.setFloorNumber(request.getFloorNumber());
        room.setCoverImagePath(request.getCoverImagePath().trim());
        room.setCapacity(request.getCapacity());
        room.setListedMonthlyRent(request.getListedMonthlyRent());
    }
}
