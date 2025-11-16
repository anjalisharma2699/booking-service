package com.cleaning.bookingservice.repository;


import com.cleaning.bookingservice.entity.BookingCleaner;
import com.cleaning.bookingservice.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface BookingCleanerRepository extends BaseRepository<BookingCleaner, Long> {
    List<BookingCleaner> findByBooking_Id(Long bookingId);
    List<BookingCleaner> findByCleaner_Id(Long cleanerId);
}