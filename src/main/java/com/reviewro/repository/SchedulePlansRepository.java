package com.reviewro.repository;

import com.reviewro.entity.SchedulePlansEntity;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchedulePlansRepository extends JpaRepository<SchedulePlansEntity, Long> {
    /* 특정 일차(dailyId)에 속한 세부 일정 중 가장 큰 순서 번호(scheduleNumber) 조회 */
    @Query("SELECT MAX(s.scheduleNumber) FROM SchedulePlansEntity s WHERE s.dailyPlansEntity.dailyId = :dailyId")
    Optional<Integer> findMaxScheduleNumberByDailyId(@Param("dailyId") Long dailyId);

    //일차별 스케줄 조회
    @Query("SELECT s FROM SchedulePlansEntity s WHERE s.dailyPlansEntity.dailyId IN :dailyIds")
    List<SchedulePlansEntity> findByDailyIdIn(@Param("dailyIds") List<Long> dailyIds);

    // 여행계획 상세용: 모든 일차의 일정을 장소까지 한 번에 (일차마다 따로 조회하지 않는다)
    @EntityGraph(attributePaths = {"location", "dailyPlansEntity"})
    @Query("SELECT s FROM SchedulePlansEntity s WHERE s.dailyPlansEntity.dailyId IN :dailyIds"
            + " ORDER BY s.dailyPlansEntity.dailyNumber, s.scheduleNumber")
    List<SchedulePlansEntity> findWithLocationByDailyIdIn(@Param("dailyIds") List<Long> dailyIds);



}
