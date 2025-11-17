package com.cleaning.bookingservice.service;

import com.cleaning.bookingservice.dto.request.CreateBookingRequest;
import com.cleaning.bookingservice.dto.request.UpdateBookingRequest;
import com.cleaning.bookingservice.dto.response.AvailabilityResponse;
import com.cleaning.bookingservice.dto.response.BookingResponse;
import com.cleaning.bookingservice.dto.response.UpdateBookingResponse;
import com.cleaning.bookingservice.entity.*;
import com.cleaning.bookingservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Mock
    private AvailabilityService  availabilityService;

    @Mock
    private BookingCleanerRepository bookingCleanerRepository;

    @Mock
    private CleanerRepository cleanerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityBlockRepository availabilityBlockRepository;


    private Vehicle vehicle(long id) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setName("Vehicle " + id);
        return v;
    }

    private CleanerProfessional cleaner(long id, long vehicleId) {
        CleanerProfessional c = new CleanerProfessional();
        c.setId(id);
        c.setName("Cleaner " + id);
        c.setVehicle(vehicle(vehicleId));
        return c;
    }

    private BookingCleaner bookingCleaner(Booking booking, CleanerProfessional cleaner) {
        BookingCleaner bc = new BookingCleaner();
        bc.setId(1L);
        bc.setBooking(booking);
        bc.setCleaner(cleaner);
        return bc;
    }

    // -----------------------------
    // TESTS START HERE
    // -----------------------------

    @Test
    void testCreateBooking_Success() {

        CreateBookingRequest req = new CreateBookingRequest();
        req.setDate("2025-11-16");
        req.setStartTime("10:00");
        req.setDurationHours(4);
        req.setRequestedCleanerCount(2);

        Vehicle v1 = vehicle(1);

        CleanerProfessional c1 = cleaner(1, 1);
        CleanerProfessional c2 = cleaner(2, 1);

        when(vehicleRepository.findAll()).thenReturn(List.of(v1));
        when(cleanerRepository.findByVehicle_Id(1L)).thenReturn(List.of(c1, c2));

        // No busy blocks → all free
        when(availabilityBlockRepository.findBlocksForCleaners(
                List.of(1L, 2L), LocalDate.of(2025, 11, 16)
        )).thenReturn(List.of());

        Booking saved = new Booking();
        saved.setId(100L);
        saved.setStartDatetime(LocalDateTime.of(2025, 11, 16, 10, 0));
        saved.setEndDatetime(LocalDateTime.of(2025, 11, 16, 14, 0));
        saved.setRequestedCleanerCount(2);

        saved.setAssignedCleaners(List.of(
                bookingCleaner(saved, c1),
                bookingCleaner(saved, c2)
        ));

        when(bookingRepository.save(any())).thenReturn(saved);

        BookingResponse resp = bookingService.createBooking(req);

        assertNotNull(resp);
        assertEquals(100L, resp.getBookingId());
        assertEquals("2025-11-16T10:00", resp.getStartDatetime().toString());
        assertEquals(2, resp.getAssignedCleanerIds().size());
    }

    @Test
    void testCreateBooking_NoCleanersAvailable() {

        CreateBookingRequest req = new CreateBookingRequest();
        req.setDate("2025-11-16");
        req.setStartTime("10:00");
        req.setDurationHours(4);
        req.setRequestedCleanerCount(2);

        Vehicle v1 = vehicle(1);

        when(vehicleRepository.findAll()).thenReturn(List.of(v1));
        when(cleanerRepository.findByVehicle_Id(1L)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(req));

        assertEquals("No available team found for requested time and cleaner count", ex.getMessage());
    }

    @Test
    void testCreateBooking_FailsOnFriday() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setDate("2025-11-14"); // Friday
        req.setStartTime("10:00");
        req.setDurationHours(2);
        req.setRequestedCleanerCount(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(req));

        assertEquals("Friday Booking is not possible", ex.getMessage());
    }

    @Test
    void testCreateBooking_InvalidDuration() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setDate("2025-11-16");
        req.setStartTime("10:00");
        req.setDurationHours(1); // invalid
        req.setRequestedCleanerCount(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(req));

        assertEquals("durationHours must be 2 or 4", ex.getMessage());
    }

    @Test
    void testCreateBooking_NoVehicleHasEnoughCleaners() {

        CreateBookingRequest req = new CreateBookingRequest();
        req.setDate("2025-11-16");
        req.setStartTime("10:00");
        req.setDurationHours(4);
        req.setRequestedCleanerCount(3);

        Vehicle v1 = vehicle(1);

        when(vehicleRepository.findAll()).thenReturn(List.of(v1));
        when(cleanerRepository.findByVehicle_Id(1L)).thenReturn(List.of(cleaner(1, 1)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(req));

        assertEquals("No available team found for requested time and cleaner count", ex.getMessage());
    }

    @Test
    void testCreateBooking_SkipsBusyCleaners() {

        CreateBookingRequest req = new CreateBookingRequest();
        req.setDate("2025-11-16");
        req.setStartTime("10:00");
        req.setDurationHours(2);
        req.setRequestedCleanerCount(1);

        Vehicle v1 = vehicle(1);
        CleanerProfessional c1 = cleaner(1, 1);
        CleanerProfessional c2 = cleaner(2, 1);

        when(vehicleRepository.findAll()).thenReturn(List.of(v1));
        when(cleanerRepository.findByVehicle_Id(1L)).thenReturn(List.of(c1, c2));

        // Create busy block for cleaner 1
        AvailabilityBlock busy = new AvailabilityBlock();
        busy.setCleanerId(1L);
        busy.setBookingId(1L);
        busy.setStartDatetime(LocalDateTime.of(2025, 11, 16, 10, 0));
        busy.setEndDatetime(LocalDateTime.of(2025, 11, 16, 12, 0));
        busy.setBlockType("BOOKED");

        // cleaner 2 has no busy blocks
        when(availabilityBlockRepository.findBlocksForCleaners(
                List.of(1L, 2L), LocalDate.of(2025, 11, 16)
        )).thenReturn(List.of(busy));

        Booking saved = new Booking();
        saved.setId(200L);
        saved.setStartDatetime(LocalDateTime.of(2025, 11, 16, 10, 0));
        saved.setEndDatetime(LocalDateTime.of(2025, 11, 16, 12, 0));

        saved.setAssignedCleaners(List.of(
                bookingCleaner(saved, c2) // only c2 assigned
        ));

        when(bookingRepository.save(any())).thenReturn(saved);

        BookingResponse res = bookingService.createBooking(req);

        assertEquals(200L, res.getBookingId());
        assertEquals(1, res.getAssignedCleanerIds().size());
        assertEquals(2L, res.getAssignedCleanerIds().get(0)); // expect free cleaner
    }


    // ---------------------------------------------------------------
// UPDATE BOOKING TESTS
// ---------------------------------------------------------------

    @Test
    void testUpdateBooking_Success() {

        Booking booking = new Booking();
        booking.setId(50L);
        booking.setStartDatetime(LocalDateTime.of(2025, 11, 16, 10, 0));
        booking.setEndDatetime(LocalDateTime.of(2025, 11, 16, 12, 0));

        CleanerProfessional c1 = cleaner(1, 1);
        BookingCleaner bc1 = bookingCleaner(booking, c1);
        booking.setAssignedCleaners(List.of(bc1));

        when(bookingRepository.findById(50L)).thenReturn(java.util.Optional.of(booking));

        // AvailabilityService response
        AvailabilityResponse.VehicleAvailability va = new AvailabilityResponse.VehicleAvailability();
        va.setVehicleId(1L);

        AvailabilityResponse.CleanerAvailability ca = new AvailabilityResponse.CleanerAvailability();
        ca.setCleanerId(1L);
        ca.setName("Cleaner 1");

        va.setCleaners(List.of(ca));

        AvailabilityResponse ar = new AvailabilityResponse();
        ar.setAvailableVehicles(List.of(va));


        UpdateBookingRequest req = new UpdateBookingRequest();
        req.setDate("2025-11-16");
        req.setStartTime("14:00");
        req.setDurationHours(2);
        req.setCleanerCount(1);

        UpdateBookingResponse response = bookingService.updateBooking(50L, req);

        assertNotNull(response);
        assertEquals(50L, response.getBookingId());
        assertEquals("Booking updated successfully", response.getMessage());

        verify(bookingRepository).save(any());
    }

    @Test
    void testUpdateBooking_BusyCleaner() {

        Booking booking = new Booking();
        booking.setId(60L);
        booking.setStartDatetime(LocalDateTime.of(2025, 11, 16, 10, 0));
        booking.setEndDatetime(LocalDateTime.of(2025, 11, 16, 12, 0));

        CleanerProfessional c1 = cleaner(1, 1);
        BookingCleaner bc1 = bookingCleaner(booking, c1);
        booking.setAssignedCleaners(List.of(bc1));

        // Old booking found
        when(bookingRepository.findById(60L)).thenReturn(java.util.Optional.of(booking));

        // Availability says cleaner *looks free*
        AvailabilityResponse.VehicleAvailability va = new AvailabilityResponse.VehicleAvailability();
        va.setVehicleId(1L);

        AvailabilityResponse.CleanerAvailability ca = new AvailabilityResponse.CleanerAvailability();
        ca.setCleanerId(1L);
        va.setCleaners(List.of(ca));

        AvailabilityResponse ar = new AvailabilityResponse();
        ar.setAvailableVehicles(List.of(va));


        // REAL overlap check → cleaner is actually busy
        when(availabilityBlockRepository.hasOverlapExcludingBooking(
                eq(1L),                               // cleanerId
                eq(60L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(true);

        UpdateBookingRequest req = new UpdateBookingRequest();
        req.setDate("2025-11-16");
        req.setStartTime("15:00");   // new time
        req.setDurationHours(2);
        req.setCleanerCount(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(60L, req));

        assertTrue(ex.getMessage().toLowerCase().contains("busy"));
    }

    @Test
    void testUpdateBooking_BookingNotFound() {

        when(bookingRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        UpdateBookingRequest req = new UpdateBookingRequest();
        req.setDate("2025-11-16");
        req.setStartTime("10:00");
        req.setDurationHours(2);
        req.setCleanerCount(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(999L, req));

        assertEquals("Booking not found: 999", ex.getMessage());
    }

}