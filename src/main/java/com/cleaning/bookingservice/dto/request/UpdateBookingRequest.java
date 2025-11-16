package com.cleaning.bookingservice.dto.request;

import com.cleaning.bookingservice.validation.ValidDurationHours;
import lombok.Data;

@Data
public class UpdateBookingRequest {
    private String date;            // yyyy-MM-dd
    private String startTime;       // HH:mm
    @ValidDurationHours
    private Integer durationHours;
    private Integer cleanerCount;
}
