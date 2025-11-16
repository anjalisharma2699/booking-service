package com.cleaning.bookingservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.cleaning.bookingservice.validation.ValidDurationHours;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBookingRequest {

    @Schema(
            description = "Date of booking",
            example = "2025-11-16",
            format = "date"
    )
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String date;

    @Schema(
            description = "Start time of booking",
            example = "18:00",
            format = "time"
    )
    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private String startTime;

    @Schema(
            description = "Duration of booking in hours (valid: 2 or 4)",
            example = "4"
    )
    @NotNull
    @ValidDurationHours
    private Integer durationHours;

    @Schema(
            description = "Requested cleaner count (1–3)",
            example = "2"
    )
    @NotNull
    private Integer requestedCleanerCount;

    @Schema(
            description = "Preferred vehicle ID (optional). If not provided, system will auto-assign.",
            example = "1",
            nullable = true
    )
    private Long preferredVehicleId;
}
