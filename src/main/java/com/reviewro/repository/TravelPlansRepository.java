package com.reviewro.repository;


import com.reviewro.entity.TravelPlansEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelPlansRepository extends JpaRepository<TravelPlansEntity, Long> {

    // 회원별 본인 일정 조회
    @Query("SELECT t FROM TravelPlansEntity t JOIN FETCH t.member WHERE t.member.memberId = :memberId")
    List<TravelPlansEntity> findByIdList(@Param("memberId") Long memberId);

    // 회원별 일정 단건 상세조회
    @Query("SELECT t FROM TravelPlansEntity t "+
            "LEFT JOIN FETCH t.dailyPlans d "+
            "WHERE t.planId = :planId AND t.member.memberId = :memberId")
    Optional<TravelPlansEntity> findByPlanIdAndMemberId(@Param("planId") Long planId, @Param("memberId") Long memberId);

    // 전체일정 조회
    @Query("SELECT t FROM TravelPlansEntity t LEFT JOIN FETCH t.member")
    List<TravelPlansEntity> findByIdListAll();

}
