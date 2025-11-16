package com.cleaning.bookingservice.entity;


import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "booking_cleaner")
@Data
public class BookingCleaner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleaner_id")
    private CleanerProfessional cleaner;
}