package com.cleaning.bookingservice.mapper;


import com.cleaning.bookingservice.dto.response.BookingResponse;
import com.cleaning.bookingservice.entity.Booking;
import com.cleaning.bookingservice.entity.BookingCleaner;


import java.util.stream.Collectors;


public class BookingMapper {
    public static BookingResponse toResponse(Booking b) {
        BookingResponse r = new BookingResponse();
        r.setBookingId(b.getId());
        r.setStartDatetime(b.getStartDatetime());
        r.setEndDatetime(b.getEndDatetime());
        r.setDurationHours(b.getDurationInHours());
        r.setAssignedCleanerIds(b.getAssignedCleaners().stream().map(BookingCleaner::getCleaner).map(c -> c.getId()).collect(Collectors.toList()));
        if (!b.getAssignedCleaners().isEmpty()) {
            r.setAssignedVehicleId(b.getAssignedCleaners().get(0).getCleaner().getVehicle().getId());
        }
        r.setStatus("CONFIRMED");
        return r;
    }
}