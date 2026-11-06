package com.reviewro.repository;

import com.reviewro.entity.DailyPlansEntity;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface DailyPlansRepository extends JpaRepository<DailyPlansEntity, Long> {

    List<DailyPlansEntity> findByTravelPlan_PlanId(Long travelPlanPlanId);

}
