package com.cleaning.bookingservice.service;

import com.cleaning.bookingservice.dto.request.CreateBookingRequest;
import com.cleaning.bookingservice.dto.request.UpdateBookingRequest;
import com.cleaning.bookingservice.dto.response.BookingResponse;
import com.cleaning.bookingservice.dto.response.UpdateBookingResponse;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);
    UpdateBookingResponse updateBooking(Long bookingId, UpdateBookingRequest req);
}
