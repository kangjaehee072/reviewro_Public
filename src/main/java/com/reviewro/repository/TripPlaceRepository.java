package com.reviewro.repository;

import com.reviewro.entity.TripPlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripPlaceRepository extends JpaRepository<TripPlaceEntity, Long> {
}
