package com.cleaning.bookingservice.service;

import com.cleaning.bookingservice.dto.request.AvailabilityRequest;
import com.cleaning.bookingservice.dto.response.AvailabilityResponse;
import com.cleaning.bookingservice.entity.AvailabilityBlock;
import com.cleaning.bookingservice.entity.CleanerProfessional;
import com.cleaning.bookingservice.entity.Vehicle;
import com.cleaning.bookingservice.repository.AvailabilityBlockRepository;
import com.cleaning.bookingservice.repository.CleanerRepository;
import com.cleaning.bookingservice.repository.VehicleRepository;
import org.antlr.v4.runtime.misc.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AvailabilityServiceImpl implements AvailabilityService {

    @Autowired
    private CleanerRepository cleanerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private AvailabilityBlockRepository availabilityBlockRepository;


    /**
     * Main API that calculates availability of vehicles and cleaners for a given date.
     * Steps:
     * 1. Reject Fridays (holiday logic).
     * 2. Identify if time-based filtering is required.
     * 3. Load all vehicles.
     * 4. For each vehicle:
     *      - Load cleaners
     *      - Fetch their availability blocks in a single query (N+1 fix)
     *      - Calculate free slots for each cleaner
     *      - If time-filter is active → verify cleaner can serve the requested timeslot
     * 5. Aggregate eligible vehicles + cleaners.
     */
    @Override
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {

        LocalDate date = LocalDate.parse(request.getDate());

        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return new AvailabilityResponse(); // empty response
        }

        boolean filterByTime = request.getStartTime() != null &&
                request.getDurationHours() != null &&
                request.getCleanerCount() != null;

        LocalTime startTime = filterByTime ? LocalTime.parse(request.getStartTime()) : null;
        LocalDateTime startDt = filterByTime ? LocalDateTime.of(date, startTime) : null;
        LocalDateTime endDt = filterByTime ? startDt.plusHours(request.getDurationHours()) : null;

        Integer requiredCleaners = request.getCleanerCount();

        List<Vehicle> vehicles = vehicleRepository.findAll();

        AvailabilityResponse response = new AvailabilityResponse();
        response.setDate(request.getDate());

        List<AvailabilityResponse.VehicleAvailability> vehicleAvailabilityList = new ArrayList<>();
        int availableVehicleCount = 0;

        for (Vehicle v : vehicles) {

            // Load cleaners belonging to the vehicle
            List<CleanerProfessional> cleaners = cleanerRepository.findByVehicle_Id(v.getId());
            if (cleaners.isEmpty()) continue;

            // Fetch all blocks for all cleaners of this vehicle in one query (N+1 fix)
            List<Long> cleanerIds = cleaners.stream().map(CleanerProfessional::getId).toList();

            Map<Long, List<AvailabilityBlock>> blocksByCleaner =
                    availabilityBlockRepository.findBlocksForCleaners(cleanerIds, date)
                            .stream()
                            .collect(Collectors.groupingBy(AvailabilityBlock::getCleanerId));

            // Ensure each cleaner has an entry
            cleanerIds.forEach(id -> blocksByCleaner.putIfAbsent(id, List.of()));

            List<AvailabilityResponse.CleanerAvailability> cleanerResponses = new ArrayList<>();
            int cleanersThatFitTime = 0;

            for (CleanerProfessional c : cleaners) {

                List<AvailabilityBlock> blocks = blocksByCleaner.get(c.getId());

                // Convert to free time slots
                List<String> freeSlots = calculateFreeSlots(blocks, date);

                // If request asks for a specific time window, validate whether cleaner fits
                if (filterByTime) {
                    boolean fits = hasFreeWindowForRange(
                            freeSlots,
                            startDt.toLocalTime(),
                            endDt.toLocalTime()
                    );
                    if (!fits) continue;
                    else cleanersThatFitTime++;
                }

                // Build cleaner response DTO
                AvailabilityResponse.CleanerAvailability ca = new AvailabilityResponse.CleanerAvailability();
                ca.setCleanerId(c.getId());
                ca.setName(c.getName());
                if (!filterByTime) {
                    ca.setAvailableSlots(freeSlots);
                }
                cleanerResponses.add(ca);
            }

            // If filtered by time → vehicle must have minimum required cleaners available
            if (filterByTime && cleanersThatFitTime < requiredCleaners) continue;

            // Add vehicle to result only if at least one cleaner is available
            if (!cleanerResponses.isEmpty()) {
                AvailabilityResponse.VehicleAvailability va = new AvailabilityResponse.VehicleAvailability();
                va.setVehicleId(v.getId());
                va.setVehicleName(v.getName());
                va.setCleaners(cleanerResponses);

                vehicleAvailabilityList.add(va);
                availableVehicleCount++;
            }
        }

        response.setAvailableVehicles(vehicleAvailabilityList);
        response.setCount(availableVehicleCount);

        return response;
    }


    /**
     * Converts busy availability blocks into free time intervals.
     * Logic:
     * 1. Convert blocks → busy minute intervals.
     * 2. Merge overlapping busy intervals.
     * 3. Subtract from working hours (8:00–22:00).
     * 4. Return free slots in "HH:mm-HH:mm" format.
     */
    public List<String> calculateFreeSlots(List<AvailabilityBlock> blocks, LocalDate date) {
        int WORK_START = 8 * 60;
        int WORK_END = 22 * 60;

        // Convert AvailabilityBlock → Interval (startMin → endMin)
        List<Interval> busy = blocks.stream()
                .filter(b -> !"FREE".equalsIgnoreCase(b.getBlockType()))
                .map(b -> new Interval(
                        b.getStartDatetime().toLocalTime().toSecondOfDay() / 60,
                        b.getEndDatetime().toLocalTime().toSecondOfDay() / 60
                ))
                .sorted(Comparator.comparingInt(i -> i.a))
                .toList();

        List<Interval> merged = mergeBusy(busy);

        List<Interval> free = new ArrayList<>();
        int current = WORK_START;

        for (Interval b : merged) {
            if (b.a > current) {
                free.add(new Interval(current, Math.min(b.a, WORK_END)));
            }
            current = Math.max(current, b.b);
        }

        if (current < WORK_END) {
            free.add(new Interval(current, WORK_END));
        }

        List<String> result = new ArrayList<>();
        for (Interval f : free) {
            result.add(formatTime(f.a) + "-" + formatTime(f.b));
        }
        return result;
    }

    /**
     * Format minutes → HH:mm
     */
    private String formatTime(int mins) {
        int h = mins / 60;
        int m = mins % 60;
        return String.format("%02d:%02d", h, m);
    }

    /**
     * Merges overlapping busy intervals into a minimal set.
     * Example:
     * (10,20), (15,30) → (10,30)
     */
    private List<Interval> mergeBusy(List<Interval> intervals) {
        if (intervals.isEmpty()) return intervals;

        List<Interval> merged = new ArrayList<>();
        Interval prev = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            Interval curr = intervals.get(i);

            if (curr.a <= prev.b) {
                prev = new Interval(prev.a, Math.max(prev.b, curr.b));
            } else {
                merged.add(prev);
                prev = curr;
            }
        }

        merged.add(prev);
        return merged;
    }

    /**
     * Checks whether the cleaner has a free slot that fully covers the requested time window.
     * Example:
     * freeSlots: ["10:00-14:00"]
     * request: 11:00–13:00 → true
     */
    private boolean hasFreeWindowForRange(List<String> freeSlots,
                                          LocalTime start,
                                          LocalTime end) {

        for (String slot : freeSlots) {
            String[] parts = slot.split("-");
            LocalTime fs = LocalTime.parse(parts[0]);
            LocalTime fe = LocalTime.parse(parts[1]);

            if (!start.isBefore(fs) && !end.isAfter(fe)) {
                return true;
            }
        }
        return false;
    }
}