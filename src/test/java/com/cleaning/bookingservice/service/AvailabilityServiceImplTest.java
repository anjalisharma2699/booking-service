package com.cleaning.bookingservice.service;

import com.cleaning.bookingservice.dto.request.AvailabilityRequest;
import com.cleaning.bookingservice.dto.response.AvailabilityResponse;
import com.cleaning.bookingservice.entity.AvailabilityBlock;
import com.cleaning.bookingservice.entity.CleanerProfessional;
import com.cleaning.bookingservice.entity.Vehicle;
import com.cleaning.bookingservice.repository.AvailabilityBlockRepository;
import com.cleaning.bookingservice.repository.CleanerRepository;
import com.cleaning.bookingservice.repository.VehicleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AvailabilityServiceImplTest {

    @Mock
    private CleanerRepository cleanerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private AvailabilityBlockRepository availabilityBlockRepository;

    @InjectMocks
    private AvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCheckAvailability_NoTimeFilters() {
        Vehicle v = new Vehicle();
        v.setId(1L);
        v.setName("Car");

        CleanerProfessional c = new CleanerProfessional();
        c.setId(10L);
        c.setName("Ayesha");

        when(vehicleRepository.findAll()).thenReturn(List.of(v));
        when(cleanerRepository.findByVehicle_Id(1L)).thenReturn(List.of(c));
        when(availabilityBlockRepository.findBlocksForCleaner(anyLong(), any()))
                .thenReturn(Collections.emptyList());

        AvailabilityRequest req = new AvailabilityRequest();
        req.setDate("2025-11-16");

        AvailabilityResponse res = service.checkAvailability(req);

        assertEquals(1, res.getAvailableVehicles().size());
        assertEquals("Car", res.getAvailableVehicles().get(0).getVehicleName());
    }

    @Test
    void testCalculateFreeSlots() {

        AvailabilityBlock booked = new AvailabilityBlock();
        booked.setCleanerId(1L);
        booked.setBookingId(1L);
        booked.setStartDatetime(Timestamp.valueOf("2025-11-16 10:00:00").toLocalDateTime());
        booked.setEndDatetime(Timestamp.valueOf("2025-11-16 12:00:00").toLocalDateTime());
        booked.setBlockType("BOOKED");

        List<AvailabilityBlock> blocks = List.of(booked);

        List<String> slots = service.calculateFreeSlots(blocks);

        assertTrue(slots.contains("08:00-10:00"));
        assertTrue(slots.contains("12:00-22:00"));
    }

    @Test
    void testAvailability_FridayShouldReturnEmpty() {

        AvailabilityRequest req = new AvailabilityRequest();
        req.setDate("2025-11-14"); // Friday

        AvailabilityResponse res = service.checkAvailability(req);

        assertEquals(null, res.getCount());
        assertTrue(res.getAvailableVehicles().isEmpty());
    }

}
