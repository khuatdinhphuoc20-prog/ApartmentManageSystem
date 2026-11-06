package com.apartment.rental.room.service.impl;

import com.apartment.rental.room.dto.response.RoomCapacityForM04Response;
import com.apartment.rental.room.dto.response.RoomOptionForM04Response;
import com.apartment.rental.room.dto.response.RoomRentalInfoForM04Response;
import com.apartment.rental.room.entity.Room;
import com.apartment.rental.room.entity.RoomStatus;
import com.apartment.rental.room.mapper.RoomMapperForM04;
import com.apartment.rental.room.repository.RoomRepositoryForM04;
import com.apartment.rental.room.service.RoomServiceForM04;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomServiceForM04Impl implements RoomServiceForM04 {
    private final RoomRepositoryForM04 roomRepositoryForM04;
    private final RoomMapperForM04 roomMapperForM04;


    @Override
    public RoomCapacityForM04Response getRoomCapacityForM04(Long roomId) {
        Room room = roomRepositoryForM04.findById(roomId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Room not found"));

        return roomMapperForM04.toCapacityResponse(room);

    }
    @Override
    @Transactional
    public void releaseRoomForM04(Long roomId) {

        Room room = roomRepositoryForM04.findById(roomId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Room not found"));

        room.setStatus(RoomStatus.AVAILABLE);

        roomRepositoryForM04.save(room);
    }
    @Override
    public RoomRentalInfoForM04Response getRoomRentalInfoForM04(Long roomId) {

        Room room = roomRepositoryForM04.findById(roomId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Room not found"));

        return new RoomRentalInfoForM04Response(
                room.getId(),
                room.getStatus(),
                room.getListedMonthlyRent()
        );
    }
    @Override
    @Transactional
    public void reserveRoomForM04(Long roomId) {

        Room room = roomRepositoryForM04.findById(roomId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Room not found"));

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new IllegalStateException("Room is not available");
        }

        room.setStatus(RoomStatus.RENTED);

        roomRepositoryForM04.save(room);
    }
    @Override
    public List<RoomOptionForM04Response> getAvailableRooms() {
        return roomRepositoryForM04.findAllByStatusOrderByRoomCodeAsc(RoomStatus.AVAILABLE)
                .stream()
                .map(roomMapperForM04::toM04OptionResponse)
                .toList();
    }
}
