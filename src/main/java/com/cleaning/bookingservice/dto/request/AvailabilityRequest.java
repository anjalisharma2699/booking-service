package com.cleaning.bookingservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.cleaning.bookingservice.validation.ValidDurationHours;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityRequest {

    @Schema(
            description = "Date to check availability",
            example = "2025-11-16",
            format = "date"
    )
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String date;

    @Schema(
            description = "Start time (optional)",
            example = "10:00",
            format = "time"
    )
    @JsonFormat(pattern = "HH:mm")
    private String startTime;

    @Schema(
            description = "Duration of work in hours (valid: 2 or 4)",
            example = "4"
    )
    @ValidDurationHours
    private Integer durationHours;

    @Schema(
            description = "Number of cleaners required (1–3)",
            example = "2"
    )
    @Min(1)
    @Max(3)
    private Integer cleanerCount;
}
