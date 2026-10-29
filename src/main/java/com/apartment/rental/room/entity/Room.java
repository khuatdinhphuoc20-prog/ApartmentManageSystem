package com.apartment.rental.room.entity;

import com.apartment.rental.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends AuditableEntity {

    @Nationalized
    @Column(name = "room_code", nullable = false, length = 30)
    private String roomCode;

    @Nationalized
    @Column(name = "description", nullable = true, length = 2000)
    private String description;

    @Column(name = "area_m2", nullable = true, precision = 8, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "floor_number", nullable = false)
    private Short floorNumber;

    @Nationalized
    @Column(name = "cover_image_path", nullable = false, length = 500)
    private String coverImagePath;

    @Column(name = "capacity", nullable = false)
    private Short capacity;

    @Column(name = "listed_monthly_rent", nullable = false, precision = 19, scale = 0)
    private BigDecimal listedMonthlyRent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RoomStatus status;
}

