package com.cleaning.bookingservice.dto.request;

import com.cleaning.bookingservice.validation.ValidDurationHours;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityRequest {
    @NotNull
    private String date; // yyyy-MM-dd

    private String startTime; // optional HH:mm

    @ValidDurationHours
    private Integer durationHours;

    @Min(1)
    @Max(3)
    private Integer cleanerCount;

}