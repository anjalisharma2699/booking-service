package com.cleaning.bookingservice.repository;


import com.cleaning.bookingservice.entity.Booking;
import com.cleaning.bookingservice.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends BaseRepository<Booking, Long> {
    List<Booking> findByStartDatetimeBetween(LocalDateTime start, LocalDateTime end);


    @Query("SELECT b FROM Booking b WHERE DATE(b.startDatetime) = :date")
    List<Booking> findBookingsByDate(@Param("date") LocalDate date);
}