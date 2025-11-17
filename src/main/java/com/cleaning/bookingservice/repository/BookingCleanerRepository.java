package com.cleaning.bookingservice.repository;


import com.cleaning.bookingservice.entity.BookingCleaner;
import com.cleaning.bookingservice.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface BookingCleanerRepository extends BaseRepository<BookingCleaner, Long> {
    List<BookingCleaner> findByBooking_Id(Long bookingId);
    List<BookingCleaner> findByCleaner_Id(Long cleanerId);
    @Modifying
    @Query("""
   UPDATE BookingCleaner bc 
   SET bc.cleaner.id = :newCleanerId
   WHERE bc.booking.id = :bookingId
     AND bc.cleaner.id = :oldCleanerId
""")
    void updateCleanerAssignment(Long bookingId, Long oldCleanerId, Long newCleanerId);


    @Modifying
    @Query("DELETE FROM BookingCleaner bc WHERE bc.booking.id = :bookingId")
    void deleteByBookingId(Long bookingId);


    List<BookingCleaner> findByBookingId(Long bookingId);

}