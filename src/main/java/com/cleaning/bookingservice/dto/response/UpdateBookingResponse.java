package com.cleaning.bookingservice.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateBookingResponse {
    private Long bookingId;
    private String message;
}
