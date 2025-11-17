package com.cleaning.bookingservice.constants;

public final class BookingServiceConstants {

    // Working Hours
    public static final int WORK_START_MINUTES = 8 * 60;   // 08:00
    public static final int WORK_END_MINUTES = 22 * 60;    // 22:00

    // Break Duration
    public static final int BREAK_MINUTES = 30;

    public static final String BOOKING_NOT_POSSIBLE_FRIDAY_ERR_MSG= "Friday Booking is not possible";

    public enum BookingBlockType {
        BOOKED,
        BREAK,
        FREE
    }
}
