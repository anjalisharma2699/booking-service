package com.cleaning.bookingservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.cleaning.bookingservice.validation.ValidDurationHours;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateBookingRequest {

    @Schema(
            description = "Updated booking date",
            example = "2025-11-18",
            format = "date"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String date;

    @Schema(
            description = "Updated booking start time",
            example = "14:00",
            format = "time"
    )
    @JsonFormat(pattern = "HH:mm")
    private String startTime;

    @Schema(
            description = "Updated booking duration (2 or 4 hours)",
            example = "2"
    )
    @ValidDurationHours
    private Integer durationHours;

    @Schema(
            description = "Updated cleaner count",
            example = "2"
    )
    private Integer cleanerCount;
}