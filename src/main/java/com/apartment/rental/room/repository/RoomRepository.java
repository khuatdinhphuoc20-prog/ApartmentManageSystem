package com.apartment.rental.room.repository;

import com.apartment.rental.room.entity.Room;
import com.apartment.rental.room.entity.RoomStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    boolean existsByRoomCode(String roomCode);

    long countByStatus(RoomStatus status);

    List<Room> findAllByStatusOrderByRoomCodeAsc(RoomStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT r FROM Room r
            WHERE r.status = 'AVAILABLE'
              AND (:keyword IS NULL OR LOWER(r.roomCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY r.floorNumber ASC, r.roomCode ASC
            """)
    List<Room> searchAvailableRooms(@Param("keyword") String keyword);

    @Query("""
            SELECT r FROM Room r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:keyword IS NULL OR LOWER(r.roomCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY r.floorNumber ASC, r.roomCode ASC
            """)
    List<Room> searchAllRoomsForAdmin(@Param("keyword") String keyword,
                                      @Param("status") RoomStatus status);

    @Query("""
            SELECT r FROM Room r
            WHERE r.status = 'AVAILABLE'
              AND (:floorNumber IS NULL OR r.floorNumber = :floorNumber)
              AND (:minPrice IS NULL OR r.listedMonthlyRent >= :minPrice)
              AND (:maxPrice IS NULL OR r.listedMonthlyRent <= :maxPrice)
              AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
              AND (:minArea IS NULL OR (r.areaM2 IS NOT NULL AND r.areaM2 >= :minArea))
              AND (:maxArea IS NULL OR (r.areaM2 IS NOT NULL AND r.areaM2 <= :maxArea))
            ORDER BY r.floorNumber ASC, r.roomCode ASC
            """)
    List<Room> filterAvailableRooms(@Param("floorNumber") Short floorNumber,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("minCapacity") Short minCapacity,
                                    @Param("minArea") BigDecimal minArea,
                                    @Param("maxArea") BigDecimal maxArea);
}
