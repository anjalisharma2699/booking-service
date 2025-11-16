package com.cleaning.bookingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NonNull;


import java.util.ArrayList;
import java.util.List;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailabilityResponse {
    private String date;
    private List<VehicleAvailability> availableVehicles= new ArrayList<>();;
    private Integer count;


    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VehicleAvailability {
        private Long vehicleId;
        private String vehicleName;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<CleanerAvailability> cleaners;
    }


    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CleanerAvailability {
        private Long cleanerId;
        private String name;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<String> availableSlots; // like "08:00-22:00"
    }
}