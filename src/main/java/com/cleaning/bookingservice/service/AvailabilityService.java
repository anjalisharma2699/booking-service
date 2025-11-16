package com.cleaning.bookingservice.service;

import com.cleaning.bookingservice.dto.request.AvailabilityRequest;
import com.cleaning.bookingservice.dto.response.AvailabilityResponse;
import org.springframework.stereotype.Service;

@Service
public interface AvailabilityService {
    AvailabilityResponse checkAvailability(AvailabilityRequest request);
}
