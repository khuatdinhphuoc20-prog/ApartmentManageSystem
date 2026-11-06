package com.apartment.rental.room.exception;

public class InvalidRoomStateException extends RuntimeException {
    public InvalidRoomStateException() {
        super("Trạng thái phòng không hợp lệ để thực hiện thao tác");
    }

    public InvalidRoomStateException(String message) {
        super(message);
    }
}
