package com.cleaning.bookingservice.dto.response;


import lombok.Data;


import java.time.LocalDateTime;
import java.util.List;


@Data
public class BookingResponse {
    private Long bookingId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Integer durationHours;
    private List<Long> assignedCleanerIds;
    private Long assignedVehicleId;
    private String status;
}