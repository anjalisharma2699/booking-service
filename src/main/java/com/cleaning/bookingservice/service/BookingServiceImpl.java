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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.cleaning.bookingservice.constants.BookingServiceConstants.BOOKING_NOT_POSSIBLE_FRIDAY_ERR_MSG;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final CleanerRepository cleanerRepository;
    private final BookingCleanerRepository bookingCleanerRepository;
    private final AvailabilityBlockRepository availabilityBlockRepository;
    private final VehicleRepository vehicleRepository;
    private final AvailabilityService availabilityService;

    @Autowired
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

        validateRequest(request);

        LocalDate date = LocalDate.parse(request.getDate());
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalDateTime startDt = LocalDateTime.of(date, startTime);
        LocalDateTime endDt = startDt.plusHours(request.getDurationHours());

        validateWorkingDay(date);
        validateWorkingHours(startTime, startDt, request.getDurationHours());

        logBookingAttempt(request, date, startTime, endDt);

        List<Long> vehicleIds = resolveVehicleOrder(request.getPreferredVehicleId());
        List<CleanerProfessional> selectedCleaners =
                findAvailableCleaners(vehicleIds, startDt, endDt, request.getRequestedCleanerCount());

        verifyCleanerStillFree(selectedCleaners, startDt, endDt);

        Booking savedBooking = saveBooking(startDt, endDt, request.getDurationHours(),
                request.getRequestedCleanerCount(), selectedCleaners);

        createAvailabilityBlocksTransactional(savedBooking, selectedCleaners);

        return BookingMapper.toResponse(savedBooking);
    }

    // VALIDATION
    private void validateRequest(CreateBookingRequest req) {
        Objects.requireNonNull(req, "request must not be null");

        if (req.getDurationHours() == null ||
                (req.getDurationHours() != 2 && req.getDurationHours() != 4)) {
            throw new IllegalArgumentException("durationHours must be 2 or 4");
        }

        if (req.getRequestedCleanerCount() == null ||
                req.getRequestedCleanerCount() < 1 ||
                req.getRequestedCleanerCount() > 3) {
            throw new IllegalArgumentException("requestedCleanerCount must be 1..3");
        }
    }

    private void validateWorkingDay(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            throw new BookingConflictException(BOOKING_NOT_POSSIBLE_FRIDAY_ERR_MSG);
        }
    }

    private void validateWorkingHours(LocalTime startTime, LocalDateTime startDt, int duration) {

        LocalTime WORK_START = LocalTime.of(8, 0);
        LocalTime WORK_END = LocalTime.of(22, 0);

        if (startTime.isBefore(WORK_START) || startTime.isAfter(WORK_END)) {
            throw new BookingConflictException("Bookings are only allowed between 08:00 and 22:00");
        }

        if (startDt.plusHours(duration).toLocalTime().isBefore(WORK_START) || startDt.plusHours(duration).toLocalTime().isAfter(WORK_END)) {
            throw new BookingConflictException("Bookings are only allowed between 08:00 and 22:00");
        }
    }


    // LOGGING
    private void logBookingAttempt(CreateBookingRequest req,
                                   LocalDate date,
                                   LocalTime startTime,
                                   LocalDateTime endDt) {

        log.info("Attempting booking on {} {} -> {} for {} cleaners (preferred vehicle={})",
                date,
                startTime,
                endDt.toLocalTime(),
                req.getRequestedCleanerCount(),
                req.getPreferredVehicleId());
    }


    // VEHICLE & CLEANER SELECTION
    private List<Long> resolveVehicleOrder(Long preferredVehicleId) {
        if (preferredVehicleId != null) {
            return List.of(preferredVehicleId);
        }
        return vehicleRepository.findAll().stream()
                .map(Vehicle::getId)
                .sorted()
                .toList();
    }

    private List<CleanerProfessional> findAvailableCleaners(
            List<Long> vehicleIds,
            LocalDateTime startDt,
            LocalDateTime endDt,
            int requestedCount) {

        for (Long vid : vehicleIds) {
            List<CleanerProfessional> cleaners = cleanerRepository.findByVehicle_Id(vid);
            if (cleaners.size() < requestedCount) continue;

            List<CleanerProfessional> freeCleaners = cleaners.stream()
                    .filter(c -> isCleanerFree(c.getId(), startDt, endDt))
                    .limit(requestedCount)
                    .toList();

            if (freeCleaners.size() == requestedCount) {
                return freeCleaners;
            }
        }

        throw new BookingConflictException("No available team found for requested time and cleaner count");
    }

    private void verifyCleanerStillFree(List<CleanerProfessional> cleaners,
                                        LocalDateTime start,
                                        LocalDateTime end) {

        for (CleanerProfessional cp : cleaners) {
            if (availabilityBlockRepository.hasOverlap(cp.getId(), start, end)) {
                throw new BookingConflictException(
                        "Cleaner " + cp.getId() + " is no longer available for the requested slot"
                );
            }
        }
    }

    // BOOKING CREATION
    private Booking saveBooking(LocalDateTime startDt,
                                LocalDateTime endDt,
                                int durationHours,
                                int cleanerCount,
                                List<CleanerProfessional> selectedCleaners) {

        Booking booking = new Booking();
        booking.setStartDatetime(startDt);
        booking.setEndDatetime(endDt);
        booking.setDurationInHours(durationHours);
        booking.setRequestedCleanerCount(cleanerCount);

        List<BookingCleaner> links = selectedCleaners.stream()
                .map(c -> {
                    BookingCleaner bc = new BookingCleaner();
                    bc.setBooking(booking);
                    bc.setCleaner(c);
                    return bc;
                })
                .toList();

        booking.setAssignedCleaners(links);

        return bookingRepository.save(booking);
    }

    // CLEANER FREE CHECK
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
            booked.setBookingId(booking.getId());
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
                breakBlock.setBookingId(booking.getId());
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

        // ---- VALIDATE inputs ----
        validateUpdateRequest(req);

        LocalDate date = LocalDate.parse(req.getDate());
        LocalTime startTime = LocalTime.parse(req.getStartTime());
        LocalDateTime newStart = LocalDateTime.of(date, startTime);
        LocalDateTime newEnd = newStart.plusHours(req.getDurationHours());

        validateWorkingDay(date);
        validateWorkingHours(startTime, newStart, req.getDurationHours());

        // ---- Get existing cleaners (unchanged) ----
        List<BookingCleaner> assignedCleaners = booking.getAssignedCleaners();
        if (assignedCleaners.isEmpty()) {
            throw new RuntimeException("Booking has no assigned cleaners.");
        }

        // ---- Validate new time window for every cleaner ----
        for (BookingCleaner bc : assignedCleaners) {

            Long cleanerId = bc.getCleaner().getId();

            boolean conflict = availabilityBlockRepository.hasOverlapExcludingBooking(
                    cleanerId,
                    bookingId,
                    newStart,
                    newEnd
            );

            if (conflict) {
                throw new BookingConflictException(
                        "Cleaner " + cleanerId + " is busy during the new requested time");
            }
        }

        // ---- UPDATE BOOKING ---
        LocalDateTime oldStart = booking.getStartDatetime();
        LocalDateTime oldEnd = booking.getEndDatetime();

        booking.setStartDatetime(newStart);
        booking.setEndDatetime(newEnd);
        booking.setDurationInHours(req.getDurationHours());
        bookingRepository.save(booking);

        // ---- UPDATE Availability Blocks ----
        for (BookingCleaner bc : assignedCleaners) {

            Long cleanerId = bc.getCleaner().getId();

            //  UPDATE existing BOOKED block
            availabilityBlockRepository.updateBlock(
                    cleanerId,
                    oldStart,
                    oldEnd,
                    newStart,
                    newEnd,
                    BookingBlockType.BOOKED.name()
            );

            //  UPDATE existing BREAK block
            availabilityBlockRepository.updateBreakBlock(
                    cleanerId,
                    oldEnd,
                    oldEnd.plusMinutes(BookingServiceConstants.BREAK_MINUTES),
                    newEnd,
                    newEnd.plusMinutes(BookingServiceConstants.BREAK_MINUTES),
                    BookingBlockType.BREAK.name()
            );
        }

        // ---- RESPONSE ----
        UpdateBookingResponse res = new UpdateBookingResponse();
        res.setBookingId(booking.getId());
        res.setMessage("Booking updated successfully");

        return res;
    }

    private void validateUpdateRequest(UpdateBookingRequest req) {

        if (req.getDurationHours() != 2 && req.getDurationHours() != 4) {
            throw new IllegalArgumentException("durationHours must be 2 or 4");
        }

        if (req.getCleanerCount() < 1 || req.getCleanerCount() > 3) {
            throw new IllegalArgumentException("cleanerCount must be 1..3");
        }
    }
}