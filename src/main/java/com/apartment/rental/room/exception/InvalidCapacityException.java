package com.apartment.rental.room.exception;

public class InvalidCapacityException extends RuntimeException {
    public InvalidCapacityException() {
        super("Sức chứa mới không được nhỏ hơn số người đang cư trú thực tế");
    }

    public InvalidCapacityException(String message) {
        super(message);
    }
}
