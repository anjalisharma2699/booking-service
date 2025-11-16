package com.cleaning.bookingservice.repository;


import com.cleaning.bookingservice.entity.CleanerProfessional;
import com.cleaning.bookingservice.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface CleanerRepository extends BaseRepository<CleanerProfessional, Long> {
    List<CleanerProfessional> findByVehicle_Id(Long vehicleId);


    @Query("SELECT c FROM CleanerProfessional c WHERE c.id IN :ids")
    List<CleanerProfessional> findAllByIds(@Param("ids") List<Long> ids);
}
