package com.cleaning.bookingservice.entity;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "vehicle")
@Data
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
}