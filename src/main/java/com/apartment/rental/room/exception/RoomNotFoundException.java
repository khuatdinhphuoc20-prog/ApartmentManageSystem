package com.apartment.rental.room.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException() {
        super("Phòng không tồn tại");
    }

    public RoomNotFoundException(String message) {
        super(message);
    }
}
