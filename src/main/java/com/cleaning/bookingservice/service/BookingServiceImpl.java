package com.cleaning.bookingservice.service;

import com.cleaning.bookingservice.constants.BookingServiceConstants;
import com.cleaning.bookingservice.constants.BookingServiceConstants.BookingBlockType;
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

import java.time.*;
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
                              VehicleRepository vehicleRepository,
                              AvailabilityService availabilityService) {

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

        Objects.requireNonNull(request, "request must not be null");

        LocalDate date = LocalDate.parse(request.getDate());
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalDateTime startDt = LocalDateTime.of(date, startTime);

        if (request.getDurationHours() == null ||
                !(request.getDurationHours() == 2 || request.getDurationHours() == 4)) {
            throw new IllegalArgumentException("durationHours must be 2 or 4");
        }

        if (request.getRequestedCleanerCount() == null ||
                request.getRequestedCleanerCount() < 1 ||
                request.getRequestedCleanerCount() > 3) {
            throw new IllegalArgumentException("requestedCleanerCount must be 1..3");
        }

        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            throw new BookingConflictException("Friday Booking is not possible");
        }

        LocalDateTime endDt = startDt.plusHours(request.getDurationHours());

        log.info("Attempting booking on {} {} -> {} for {} cleaners (preferred vehicle={})",
                date, startTime, endDt.toLocalTime(),
                request.getRequestedCleanerCount(),
                request.getPreferredVehicleId()
        );

        List<Long> vehicleIds = new ArrayList<>();

        if (request.getPreferredVehicleId() != null) {
            vehicleIds.add(request.getPreferredVehicleId());
        } else {
            vehicleRepository.findAll().stream()
                    .map(Vehicle::getId)
                    .sorted()
                    .forEach(vehicleIds::add);
        }

        List<CleanerProfessional> selectedCleaners = null;
        Long selectedVehicleId = null;

        for (Long vid : vehicleIds) {

            List<CleanerProfessional> cleaners = cleanerRepository.findByVehicle_Id(vid);
            if (cleaners.size() < request.getRequestedCleanerCount()) continue;

            List<CleanerProfessional> freeCleaners =
                    cleaners.stream()
                            .filter(c -> isCleanerFree(c.getId(), startDt, endDt))
                            .collect(Collectors.toList());

            if (freeCleaners.size() >= request.getRequestedCleanerCount()) {
                selectedCleaners = new ArrayList<>(freeCleaners.subList(0, request.getRequestedCleanerCount()));
                selectedVehicleId = vid;
                break;
            }
        }

        if (selectedCleaners == null) {
            throw new BookingConflictException("No available team found for requested time and cleaner count");
        }

        for (CleanerProfessional cp : selectedCleaners) {
            if (availabilityBlockRepository.hasOverlap(cp.getId(), startDt, endDt)) {
                throw new BookingConflictException(
                        "Cleaner " + cp.getId() + " is no longer available for the requested slot");
            }
        }

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

        Booking saved = bookingRepository.save(booking);

        createAvailabilityBlocksTransactional(saved, selectedCleaners);

        return BookingMapper.toResponse(saved);
    }

    private boolean isCleanerFree(Long cleanerId, LocalDateTime start, LocalDateTime end) {
        return !availabilityBlockRepository.hasOverlap(cleanerId, start, end);
    }

    private void createAvailabilityBlocksTransactional(Booking booking, List<CleanerProfessional> cleaners) {

        LocalDateTime start = booking.getStartDatetime();
        LocalDateTime end = booking.getEndDatetime();
        LocalDateTime breakStart = end;
        LocalDateTime breakEnd = end.plusMinutes(BookingServiceConstants.BREAK_MINUTES);

        for (CleanerProfessional cleaner : cleaners) {

            boolean bookedExists =
                    availabilityBlockRepository.existsByCleanerIdAndStartDatetimeAndEndDatetime(
                            cleaner.getId(), start, end
                    );

            if (bookedExists) {
                throw new BookingConflictException(
                        "Cleaner " + cleaner.getId() + " already has a booking at the requested time");
            }

            AvailabilityBlock booked = new AvailabilityBlock();
            booked.setCleanerId(cleaner.getId());
            booked.setStartDatetime(start);
            booked.setEndDatetime(end);
            booked.setBlockType(BookingBlockType.BOOKED.name());

            availabilityBlockRepository.save(booked);

            boolean breakExists =
                    availabilityBlockRepository.existsByCleanerIdAndStartDatetimeAndEndDatetime(
                            cleaner.getId(), breakStart, breakEnd
                    );

            if (!breakExists) {
                AvailabilityBlock breakBlock = new AvailabilityBlock();
                breakBlock.setCleanerId(cleaner.getId());
                breakBlock.setStartDatetime(breakStart);
                breakBlock.setEndDatetime(breakEnd);
                breakBlock.setBlockType(BookingBlockType.BREAK.name());

                availabilityBlockRepository.save(breakBlock);
            }
        }
    }

    @Transactional
    @Override
    public UpdateBookingResponse updateBooking(Long bookingId, UpdateBookingRequest req) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        LocalDate date = LocalDate.parse(req.getDate());
        LocalTime start = LocalTime.parse(req.getStartTime());
        LocalDateTime startDt = LocalDateTime.of(date, start);
        LocalDateTime endDt = startDt.plusHours(req.getDurationHours());

        booking.setStartDatetime(startDt);
        booking.setEndDatetime(endDt);
        booking.setDurationInHours(req.getDurationHours());
        booking.setRequestedCleanerCount(req.getCleanerCount());

        CleanerProfessional firstCleaner = booking.getAssignedCleaners().get(0).getCleaner();
        Long vehicleId = firstCleaner.getVehicle().getId();

        AvailabilityRequest aReq = new AvailabilityRequest();
        aReq.setDate(req.getDate());
        aReq.setStartTime(req.getStartTime());
        aReq.setDurationHours(req.getDurationHours());
        aReq.setCleanerCount(req.getCleanerCount());

        AvailabilityResponse availability = availabilityService.checkAvailability(aReq);

        AvailabilityResponse.VehicleAvailability vehicleAvailability =
                availability.getAvailableVehicles().stream()
                        .filter(v -> v.getVehicleId().equals(vehicleId))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Vehicle not available for updated time"));

        if (vehicleAvailability.getCleaners().size() < req.getCleanerCount()) {
            throw new RuntimeException("Not enough cleaners available for updated booking.");
        }

        List<CleanerProfessional> newCleaners =
                vehicleAvailability.getCleaners().stream()
                        .limit(req.getCleanerCount())
                        .map(c -> cleanerRepository.findById(c.getCleanerId()).get())
                        .collect(Collectors.toList());

        List<BookingCleaner> oldAssignments = booking.getAssignedCleaners();

        for (BookingCleaner ac : oldAssignments) {

            availabilityBlockRepository.deleteBlocksFor(
                    ac.getId(), startDt, endDt, BookingBlockType.BOOKED.name()
            );

            availabilityBlockRepository.deleteBlocksFor(
                    ac.getId(),
                    endDt,
                    endDt.plusMinutes(BookingServiceConstants.BREAK_MINUTES),
                    BookingBlockType.BREAK.name()
            );
        }

        bookingCleanerRepository.deleteAll(oldAssignments);
        booking.getAssignedCleaners().clear();

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
                    BookingBlockType.BOOKED.name()
            );
        }

        booking.setAssignedCleaners(newAssignments);
        bookingRepository.save(booking);

        UpdateBookingResponse res = new UpdateBookingResponse();
        res.setBookingId(booking.getId());
        res.setMessage("Booking updated successfully");

        return res;
    }
}