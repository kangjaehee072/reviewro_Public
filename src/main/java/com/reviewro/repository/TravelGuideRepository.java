package com.reviewro.repository;

import com.reviewro.entity.TravelGuideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelGuideRepository extends JpaRepository<TravelGuideEntity, Long> {

    Optional<TravelGuideEntity> findByPlanId(Long planId);
}
