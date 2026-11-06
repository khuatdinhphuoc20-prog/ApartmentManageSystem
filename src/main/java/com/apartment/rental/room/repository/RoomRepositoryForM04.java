package com.apartment.rental.room.repository;

import com.apartment.rental.room.entity.Room;
import com.apartment.rental.room.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepositoryForM04 extends JpaRepository<Room,Long> {
    List<Room> findAllByStatusOrderByRoomCodeAsc(RoomStatus status);
}
