package com.cleaning.bookingservice.controller;

import com.cleaning.bookingservice.dto.request.AvailabilityRequest;
import com.cleaning.bookingservice.dto.response.AvailabilityResponse;
import com.cleaning.bookingservice.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    @Autowired
    private AvailabilityService availabilityService;

    @PostMapping
    public ResponseEntity<AvailabilityResponse> checkAvailability(@Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.checkAvailability(request));
    }
}