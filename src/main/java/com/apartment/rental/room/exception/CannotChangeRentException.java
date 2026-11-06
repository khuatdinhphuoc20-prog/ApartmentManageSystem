package com.apartment.rental.room.exception;

public class CannotChangeRentException extends RuntimeException {
    public CannotChangeRentException() {
        super("Không được phép thay đổi giá niêm yết khi phòng đang có hợp đồng chờ hiệu lực hoặc đang hoạt động");
    }

    public CannotChangeRentException(String message) {
        super(message);
    }
}
