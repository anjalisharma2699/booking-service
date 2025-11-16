package com.cleaning.bookingservice.exception;

public class BookingConflictException extends RuntimeException {
    public BookingConflictException() { super(); }
    public BookingConflictException(String message) { super(message); }
    public BookingConflictException(String message, Throwable cause) { super(message, cause); }
}
