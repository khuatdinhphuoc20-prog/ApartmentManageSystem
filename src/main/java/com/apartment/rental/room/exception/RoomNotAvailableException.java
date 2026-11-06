package com.apartment.rental.room.exception;

public class RoomNotAvailableException extends RuntimeException {
    public RoomNotAvailableException() {
        super("Phòng không khả dụng");
    }

    public RoomNotAvailableException(String message) {
        super(message);
    }
}
