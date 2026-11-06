package com.reviewro.repository;

import com.reviewro.entity.TravelGroupPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelGroupPlanRepository extends JpaRepository<TravelGroupPlanEntity, Long> {

    // 그룹에 공유된 계획 목록 (최근에 공유한 순)
    @Query("SELECT gp FROM TravelGroupPlanEntity gp JOIN FETCH gp.plan JOIN FETCH gp.sharedBy"
            + " WHERE gp.group.groupId = :groupId ORDER BY gp.groupPlanId DESC")
    List<TravelGroupPlanEntity> findByGroup(@Param("groupId") Long groupId);

    Optional<TravelGroupPlanEntity> findByGroup_GroupIdAndPlan_PlanId(Long groupId, Long planId);

    boolean existsByGroup_GroupIdAndPlan_PlanId(Long groupId, Long planId);

    void deleteByGroup_GroupId(Long groupId);

    // 계획을 지우기 전에 공유 기록부터 지운다
    void deleteByPlan_PlanId(Long planId);

    // 그룹을 나가면 내가 공유한 계획도 내린다
    void deleteByGroup_GroupIdAndSharedBy_MemberId(Long groupId, Long memberId);
}
