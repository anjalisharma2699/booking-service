package com.cleaning.bookingservice.repository.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    default boolean existsByIdValue(ID id) {
        return this.findById(id).isPresent();
    }

    default void logEntity(T entity) {
        System.out.println("Entity: " + entity.toString());
    }
}
