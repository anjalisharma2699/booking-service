package com.cleaning.bookingservice.service;

import com.cleaning.bookingservice.dto.request.AvailabilityRequest;
import com.cleaning.bookingservice.dto.request.CreateBookingRequest;
import com.cleaning.bookingservice.dto.request.UpdateBookingRequest;
import com.cleaning.bookingservice.dto.response.AvailabilityResponse;
import com.cleaning.bookingservice.dto.response.BookingResponse;
import com.cleaning.bookingservice.dto.response.UpdateBookingResponse;
import com.cleaning.bookingservice.entity.*;
import com.cleaning.bookingservice.exception.BookingConflictException;
import com.cleaning.bookingservice.mapper.BookingMapper;
import com.cleaning.bookingservice.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final CleanerRepository cleanerRepository;
    private final BookingCleanerRepository bookingCleanerRepository;
    private final AvailabilityBlockRepository availabilityBlockRepository;
    private final VehicleRepository vehicleRepository;
    private final AvailabilityService availabilityService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              CleanerRepository cleanerRepository,
                              BookingCleanerRepository bookingCleanerRepository,
                              AvailabilityBlockRepository availabilityBlockRepository,
                              VehicleRepository vehicleRepository, AvailabilityService availabilityService) {
        this.bookingRepository = bookingRepository;
        this.cleanerRepository = cleanerRepository;
        this.bookingCleanerRepository = bookingCleanerRepository;
        this.availabilityBlockRepository = availabilityBlockRepository;
        this.vehicleRepository = vehicleRepository;
        this.availabilityService = availabilityService;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        // parse and validate input
        Objects.requireNonNull(request, "request must not be null");
        LocalDate date = LocalDate.parse(request.getDate());
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        if (request.getDurationHours() == null || !(request.getDurationHours() == 2 || request.getDurationHours() == 4)) {
            throw new IllegalArgumentException("durationHours must be 2 or 4");
        }
        if (request.getRequestedCleanerCount() == null || request.getRequestedCleanerCount() < 1 || request.getRequestedCleanerCount() > 3) {
            throw new IllegalArgumentException("requestedCleanerCount must be 1..3");
        }

        LocalDateTime startDt = LocalDateTime.of(date, startTime);
        LocalDateTime endDt = startDt.plusHours(request.getDurationHours());
        LocalDate localDate=startDt.toLocalDate();
        if(localDate.getDayOfWeek().equals(DayOfWeek.FRIDAY)){
            throw new BookingConflictException("Friday Booking is not possible");
        }

        log.info("Attempting booking on {} {} -> {} for {} cleaners (preferred vehicle={})",
                date, startTime, endDt.toLocalTime(), request.getRequestedCleanerCount(), request.getPreferredVehicleId());

        // STEP A: Build vehicle candidate list
        List<Long> vehicleIds = new ArrayList<>();
        if (request.getPreferredVehicleId() != null) {
            vehicleIds.add(request.getPreferredVehicleId());
        } else {
            // use all vehicles (ordered by id) - you can change ordering rule as needed
            vehicleRepository.findAll().stream()
                    .map(Vehicle::getId)
                    .sorted()
                    .forEach(vehicleIds::add);
        }

        // STEP B: For each vehicle, find actual free cleaners for the requested time
        List<CleanerProfessional> selectedCleaners = null;
        Long selectedVehicleId = null;

        for (Long vid : vehicleIds) {
            List<CleanerProfessional> cleanersInVehicle = cleanerRepository.findByVehicle_Id(vid);
            if (cleanersInVehicle.size() < request.getRequestedCleanerCount()) {
                // not enough cleaners in total for this vehicle
                continue;
            }

            // filter cleaners who are free (no overlapping availability blocks)
            List<CleanerProfessional> freeCleaners = cleanersInVehicle.stream()
                    .filter(c -> isCleanerFree(c.getId(), startDt, endDt))
                    .collect(Collectors.toList());

            if (freeCleaners.size() >= request.getRequestedCleanerCount()) {
                // pick first N free cleaners deterministically
                selectedCleaners = new ArrayList<>(freeCleaners.subList(0, request.getRequestedCleanerCount()));
                selectedVehicleId = vid;
                break;
            }
        }

        if (selectedCleaners == null || selectedCleaners.size() < request.getRequestedCleanerCount()) {
            log.warn("No vehicle found with {} free cleaners for range {} - {}", request.getRequestedCleanerCount(), startDt, endDt);
            throw new BookingConflictException("No available team found for requested time and cleaner count");
        }

        // Double-check: ensure none of the selected cleaners got blocked between check and now
        for (CleanerProfessional cp : selectedCleaners) {
            boolean overlap = availabilityBlockRepository.hasOverlap(cp.getId(), startDt, endDt);
            if (overlap) {
                log.warn("Cleaner {} became busy during reservation attempt", cp.getId());
                throw new BookingConflictException("Cleaner " + cp.getId() + " is no longer available for the requested slot");
            }
        }

        // STEP C: Create Booking entity and assigned BookingCleaner entries
        Booking booking = new Booking();
        booking.setStartDatetime(startDt);
        booking.setEndDatetime(endDt);
        booking.setDurationInHours(request.getDurationHours());
        booking.setRequestedCleanerCount(request.getRequestedCleanerCount());

        List<BookingCleaner> bookingCleaners = new ArrayList<>();
        for (CleanerProfessional chosen : selectedCleaners) {
            BookingCleaner bc = new BookingCleaner();
            bc.setBooking(booking);
            bc.setCleaner(chosen);
            bookingCleaners.add(bc);
        }
        booking.setAssignedCleaners(bookingCleaners);

        Booking saved = bookingRepository.save(booking); // cascade will save booking_cleaner rows

        // STEP D: Create BOOKED and BREAK blocks (fail if identical block exists)
        createAvailabilityBlocksTransactional(saved, selectedCleaners);

        log.info("Booking {} created for vehicle {} with cleaners {}", saved.getId(), selectedVehicleId,
                selectedCleaners.stream().map(CleanerProfessional::getId).toList());

        return BookingMapper.toResponse(saved);
    }

    /**
     * Check whether a cleaner has any overlapping availability block with the given time window.
     * Overlap definition: a.start < end && a.end > start
     */
    private boolean isCleanerFree(Long cleanerId, LocalDateTime start, LocalDateTime end) {
        return !availabilityBlockRepository.hasOverlap(cleanerId, start, end);
    }

    /**
     * Creates BOOKED and BREAK blocks for each assigned cleaner.
     * If identical BOOKED block already exists, this method fails with BookingConflictException
     * to honor the business rule that identical booking must fail.
     */
    private void createAvailabilityBlocksTransactional(Booking booking, List<CleanerProfessional> BookingCleaners) {
        LocalDateTime start = booking.getStartDatetime();
        LocalDateTime end = booking.getEndDatetime();
        LocalDateTime breakStart = end;
        LocalDateTime breakEnd = end.plusMinutes(30);

        for (CleanerProfessional cleaner : BookingCleaners) {
            // If any BOOKED block already exists for this cleaner at the same exact time window -> fail
            boolean bookedExists = availabilityBlockRepository.existsByCleanerIdAndStartDatetimeAndEndDatetime(cleaner.getId(), start, end);

            if (bookedExists) {
                // We must fail the whole booking (business rule)
                log.warn("Found identical BOOKED block for cleaner {} at {}-{}. Aborting booking.", cleaner.getId(), start, end);
                throw new BookingConflictException("Cleaner " + cleaner.getId() + " already has a booking at the requested time");
            }

            // safe to insert BOOKED
            AvailabilityBlock booked = new AvailabilityBlock();
            booked.setCleanerId(cleaner.getId());
            booked.setStartDatetime(start);
            booked.setEndDatetime(end);
            booked.setBlockType("BOOKED");
            availabilityBlockRepository.save(booked);

            // For BREAK block we insert only if doesn't already exist (we don't treat existing break as reason to fail)
            boolean breakExists = availabilityBlockRepository
                    .existsByCleanerIdAndStartDatetimeAndEndDatetime(cleaner.getId(), breakStart, breakEnd);

            if (!breakExists) {
                AvailabilityBlock breakBlock = new AvailabilityBlock();
                breakBlock.setCleanerId(cleaner.getId());
                breakBlock.setStartDatetime(breakStart);
                breakBlock.setEndDatetime(breakEnd);
                breakBlock.setBlockType("BREAK");
                availabilityBlockRepository.save(breakBlock);
            }
        }
    }

    @Transactional
    @Override
    public UpdateBookingResponse updateBooking(Long bookingId, UpdateBookingRequest req) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // --- 1. Compute New Times ---
        LocalDate date = LocalDate.parse(req.getDate());
        LocalTime start = LocalTime.parse(req.getStartTime());
        LocalDateTime startDt = LocalDateTime.of(date, start);
        LocalDateTime endDt = startDt.plusHours(req.getDurationHours());

        booking.setStartDatetime(startDt);
        booking.setEndDatetime(endDt);
        booking.setDurationInHours(req.getDurationHours());
        booking.setRequestedCleanerCount(req.getCleanerCount());

        // --- 2. Determine Vehicle From Assigned Cleaners ---
        if (booking.getAssignedCleaners().isEmpty()) {
            throw new RuntimeException("Booking has no assigned cleaners.");
        }

        CleanerProfessional firstCleaner = booking.getAssignedCleaners().get(0).getCleaner();
        Long vehicleId = firstCleaner.getVehicle().getId();

        // --- 3. Check Availability for New Time ---
        AvailabilityRequest aReq = new AvailabilityRequest();
        aReq.setDate(req.getDate());
        aReq.setStartTime(req.getStartTime());
        aReq.setDurationHours(req.getDurationHours());
        aReq.setCleanerCount(req.getCleanerCount());

        AvailabilityResponse availability = availabilityService.checkAvailability(aReq);

        AvailabilityResponse.VehicleAvailability vehicleAvailability =
                availability.getAvailableVehicles()
                        .stream()
                        .filter(v -> v.getVehicleId().equals(vehicleId))
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException("Vehicle not available for this updated time."));

        if (vehicleAvailability.getCleaners().size() < req.getCleanerCount()) {
            throw new RuntimeException("Not enough cleaners available for updated booking.");
        }

        // --- 4. Get N cleaners ---
        List<CleanerProfessional> newCleaners =
                vehicleAvailability.getCleaners()
                        .stream()
                        .limit(req.getCleanerCount())
                        .map(ca -> cleanerRepository.findById(ca.getCleanerId()).get())
                        .collect(Collectors.toList());

        // --- 5. Remove old availability blocks ---
        List<BookingCleaner> oldAssignments = booking.getAssignedCleaners();
        for (BookingCleaner ac : oldAssignments) {
            availabilityBlockRepository.deleteBlocksFor(
                    ac.getId(),
                    startDt,
                    endDt,
                    "BOOKED"
            );
            availabilityBlockRepository.deleteBlocksFor(
                    ac.getId(),
                    endDt,
                    endDt.plusMinutes(30),
                    "BREAK"
            );


        }

        bookingCleanerRepository.deleteAll(oldAssignments);
        booking.getAssignedCleaners().clear();

        // --- 6. Add new cleaners ---
        List<BookingCleaner> newAssignments = new ArrayList<>();
        for (CleanerProfessional c : newCleaners) {
            BookingCleaner ac = new BookingCleaner();
            ac.setCleaner(c);
            ac.setBooking(booking);
            bookingCleanerRepository.save(ac);
            newAssignments.add(ac);

            availabilityBlockRepository.insertBlock(
                    c.getId(),
                    startDt,
                    endDt,
                    "BOOKED"
            );
        }

        booking.setAssignedCleaners(newAssignments);

        bookingRepository.save(booking);

        // --- 7. Prepare response ---
        UpdateBookingResponse res = new UpdateBookingResponse();
        res.setBookingId(booking.getId());
        res.setMessage("Booking updated successfully");

        return res;
    }


}

