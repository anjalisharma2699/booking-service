package com.cleaning.bookingservice.controller;

import com.cleaning.bookingservice.dto.request.CreateBookingRequest;
import com.cleaning.bookingservice.dto.request.UpdateBookingRequest;
import com.cleaning.bookingservice.dto.response.BookingResponse;
import com.cleaning.bookingservice.dto.response.UpdateBookingResponse;
import com.cleaning.bookingservice.service.BookingService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @PutMapping("/{bookingId}")
    public UpdateBookingResponse updateBooking(
            @PathVariable Long bookingId,
            @RequestBody UpdateBookingRequest request
    ) {
        return bookingService.updateBooking(bookingId, request);
    }


    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }
}