package com.cleaning.bookingservice.repository;

import com.cleaning.bookingservice.entity.AvailabilityBlock;
import com.cleaning.bookingservice.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilityBlockRepository extends BaseRepository<AvailabilityBlock, Long> {



    @Query("""
        SELECT a
        FROM AvailabilityBlock a
        WHERE a.cleanerId IN :cleanerIds
          AND DATE(a.startDatetime) = :date
        ORDER BY a.startDatetime
    """)
    List<AvailabilityBlock> findBlocksForCleaners(
            @Param("cleanerIds") List<Long> cleanerIds,
            @Param("date") LocalDate date
    );



    @Query("""
        SELECT a
        FROM AvailabilityBlock a
        WHERE a.cleanerId = :cleanerId
          AND DATE(a.startDatetime) = :date
        ORDER BY a.startDatetime
    """)
    List<AvailabilityBlock> findBlocksForCleaner(
            @Param("cleanerId") Long cleanerId,
            @Param("date") LocalDate date
    );



    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
        FROM AvailabilityBlock a
        WHERE a.cleanerId = :cleanerId
          AND a.startDatetime < :end
          AND a.endDatetime > :start
    """)
    boolean hasOverlap(@Param("cleanerId") Long cleanerId,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end);



    boolean existsByCleanerIdAndStartDatetimeAndEndDatetime(
            Long cleanerId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
    FROM AvailabilityBlock a
    WHERE a.cleanerId = :cleanerId
      AND a.bookingId <> :bookingId
      AND a.startDatetime < :end
      AND a.endDatetime > :start
""")
    boolean hasOverlapExcludingBooking(Long cleanerId,
                                       Long bookingId,
                                       LocalDateTime start,
                                       LocalDateTime end);



    @Modifying
    @Query("""
       UPDATE AvailabilityBlock a
       SET a.startDatetime = :newStart,
           a.endDatetime = :newEnd
       WHERE a.cleanerId = :cleanerId
         AND a.startDatetime = :oldStart
         AND a.endDatetime = :oldEnd
         AND a.blockType = :type
       """)
    void updateBlock(Long cleanerId,
                     LocalDateTime oldStart,
                     LocalDateTime oldEnd,
                     LocalDateTime newStart,
                     LocalDateTime newEnd,
                     String type);


    @Modifying
    @Query("""
       UPDATE AvailabilityBlock a
       SET a.startDatetime = :newBreakStart,
           a.endDatetime = :newBreakEnd
       WHERE a.cleanerId = :cleanerId
         AND a.startDatetime = :oldBreakStart
         AND a.endDatetime = :oldBreakEnd
         AND a.blockType = :type
       """)
    void updateBreakBlock(Long cleanerId,
                          LocalDateTime oldBreakStart,
                          LocalDateTime oldBreakEnd,
                          LocalDateTime newBreakStart,
                          LocalDateTime newBreakEnd,
                          String type);

}
