package com.apartment.rental.room.service;

import com.apartment.rental.room.dto.RoomSummaryDto;
import com.apartment.rental.room.repository.RoomRepository;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Supplies M05 with room labels for the billing form. */
@Service
public class RoomSummaryService {

    private final RoomRepository roomRepository;

    public RoomSummaryService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, RoomSummaryDto> findByRoomIds(Collection<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Map.of();
        }
        return roomRepository.findAllById(roomIds).stream()
            .map(room -> new RoomSummaryDto(room.getId(), room.getRoomCode()))
            .collect(Collectors.toMap(RoomSummaryDto::roomId, Function.identity()));
    }
}
