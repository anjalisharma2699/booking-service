package com.cleaning.bookingservice.service;

import com.cleaning.bookingservice.constants.BookingServiceConstants;
import com.cleaning.bookingservice.constants.BookingServiceConstants.BookingBlockType;
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

    @Override
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {

        LocalDate date = LocalDate.parse(request.getDate());

        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return new AvailabilityResponse();
        }

        boolean filterByTime =
                request.getStartTime() != null &&
                        request.getDurationHours() != null &&
                        request.getCleanerCount() != null;

        LocalTime startTime = filterByTime ? LocalTime.parse(request.getStartTime()) : null;
        LocalDateTime startDt = filterByTime ? LocalDateTime.of(date, startTime) : null;
        LocalDateTime endDt = filterByTime ? startDt.plusHours(request.getDurationHours()) : null;

        Integer requiredCleaners = request.getCleanerCount();

        AvailabilityResponse response = new AvailabilityResponse();
        response.setDate(request.getDate());

        List<AvailabilityResponse.VehicleAvailability> vehiclesResponse = new ArrayList<>();
        int availableVehicleCount = 0;

        for (Vehicle v : vehicleRepository.findAll()) {

            List<CleanerProfessional> cleaners = cleanerRepository.findByVehicle_Id(v.getId());
            if (cleaners.isEmpty()) continue;

            List<Long> cleanerIds = cleaners.stream().map(CleanerProfessional::getId).toList();

            Map<Long, List<AvailabilityBlock>> blocksByCleaner =
                    availabilityBlockRepository.findBlocksForCleaners(cleanerIds, date)
                            .stream()
                            .collect(Collectors.groupingBy(AvailabilityBlock::getCleanerId));

            cleanerIds.forEach(id -> blocksByCleaner.putIfAbsent(id, List.of()));

            List<AvailabilityResponse.CleanerAvailability> cleanerDtos = new ArrayList<>();
            int cleanersThatFit = 0;

            for (CleanerProfessional c : cleaners) {

                List<AvailabilityBlock> blocks = blocksByCleaner.get(c.getId());

                List<String> freeSlots = calculateFreeSlots(blocks);

                if (filterByTime) {

                    boolean fits = hasFreeWindowForRange(
                            freeSlots,
                            startDt.toLocalTime(),
                            endDt.toLocalTime()
                    );

                    if (!fits) continue;
                    else cleanersThatFit++;
                }

                AvailabilityResponse.CleanerAvailability ca = new AvailabilityResponse.CleanerAvailability();
                ca.setCleanerId(c.getId());
                ca.setName(c.getName());

                if (!filterByTime) {
                    ca.setAvailableSlots(freeSlots);
                }

                cleanerDtos.add(ca);
            }

            if (filterByTime && cleanersThatFit < requiredCleaners) continue;

            if (!cleanerDtos.isEmpty()) {
                AvailabilityResponse.VehicleAvailability va = new AvailabilityResponse.VehicleAvailability();
                va.setVehicleId(v.getId());
                va.setVehicleName(v.getName());
                va.setCleaners(cleanerDtos);

                vehiclesResponse.add(va);
                availableVehicleCount++;
            }
        }

        response.setAvailableVehicles(vehiclesResponse);
        response.setCount(availableVehicleCount);

        return response;
    }

    public List<String> calculateFreeSlots(List<AvailabilityBlock> blocks) {

        int WORK_START = BookingServiceConstants.WORK_START_MINUTES;
        int WORK_END = BookingServiceConstants.WORK_END_MINUTES;

        List<Interval> busy = blocks.stream()
                .filter(b -> !BookingBlockType.FREE.name().equalsIgnoreCase(b.getBlockType()))
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

    private String formatTime(int mins) {
        return String.format("%02d:%02d", mins / 60, mins % 60);
    }

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

    private boolean hasFreeWindowForRange(List<String> freeSlots, LocalTime start, LocalTime end) {

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