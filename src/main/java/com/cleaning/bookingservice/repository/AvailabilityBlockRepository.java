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



    @Modifying
    @Query(value = """
        INSERT INTO availability_blocks (cleaner_id, start_datetime, end_datetime, block_type)
        VALUES (:cleanerId, :start, :end, :type)
        """, nativeQuery = true)
    void insertBlock(@Param("cleanerId") Long cleanerId,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end,
                     @Param("type") String type);

    @Modifying
    @Query(value = """
        DELETE FROM availability_blocks
        WHERE cleaner_id = :cleanerId
          AND start_datetime = :start
          AND end_datetime = :end
          AND block_type = :type
        """, nativeQuery = true)
    void deleteBlocksFor(@Param("cleanerId") Long cleanerId,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end,
                         @Param("type") String type);

}
