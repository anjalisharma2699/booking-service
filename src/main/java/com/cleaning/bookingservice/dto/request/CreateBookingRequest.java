package com.cleaning.bookingservice.dto.request;


import com.cleaning.bookingservice.validation.ValidDurationHours;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CreateBookingRequest {
    @NotNull
    private String date; // yyyy-MM-dd
    @NotNull
    private String startTime; // HH:mm
    @NotNull
    @ValidDurationHours
    private Integer durationHours; // 2 or 4
    @NotNull
    private Integer requestedCleanerCount; // 1-3
    private Long preferredVehicleId; // optional
}