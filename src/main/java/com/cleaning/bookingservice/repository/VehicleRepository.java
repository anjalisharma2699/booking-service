package com.cleaning.bookingservice.repository;


import com.cleaning.bookingservice.entity.Vehicle;
import com.cleaning.bookingservice.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends BaseRepository<Vehicle, Long> {
}