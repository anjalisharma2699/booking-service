package com.cleaning.bookingservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;


@Entity
@Table(name = "availability_blocks")
@Data
public class AvailabilityBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long cleanerId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String blockType;
}