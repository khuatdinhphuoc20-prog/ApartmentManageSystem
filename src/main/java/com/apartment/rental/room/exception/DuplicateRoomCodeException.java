package com.apartment.rental.room.exception;

public class DuplicateRoomCodeException extends RuntimeException {
    public DuplicateRoomCodeException() {
        super("Mã phòng đã tồn tại trong hệ thống");
    }

    public DuplicateRoomCodeException(String message) {
        super(message);
    }
}
