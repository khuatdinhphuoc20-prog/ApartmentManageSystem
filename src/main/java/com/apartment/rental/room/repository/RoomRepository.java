package com.apartment.rental.room.repository;

import com.apartment.rental.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository owned by the Room module (M02). */
public interface RoomRepository extends JpaRepository<Room, Long> {
}
