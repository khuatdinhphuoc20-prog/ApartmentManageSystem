package com.apartment.rental.application.exception;

public class RentalApplicationException extends RuntimeException {
    public RentalApplicationException(String message) {
        super(message);
    }

    public RentalApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class NotFound extends RentalApplicationException {
        public NotFound(String message) {
            super(message);
        }
        public NotFound(Long id) {
            super("Rental application not found with ID: " + id);
        }
    }

    public static class InvalidState extends RentalApplicationException {
        public InvalidState(String message) {
            super(message);
        }
    }

    public static class Conflict extends RentalApplicationException {
        public Conflict(String message) {
            super(message);
        }
    }

    public static class Validation extends RentalApplicationException {
        public Validation(String message) {
            super(message);
        }
    }
}
