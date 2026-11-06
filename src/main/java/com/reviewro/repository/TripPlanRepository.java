package com.reviewro.repository;

import com.reviewro.entity.TripPlanEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripPlanRepository extends JpaRepository<TripPlanEntity, Long> {

    @Query("SELECT tp FROM TripPlanEntity tp LEFT JOIN FETCH tp.member WHERE tp.tripplanId = :id")
    Optional<TripPlanEntity> findByIdWithMember(@Param("id") Long id);

    // 공개 글과 내 글만 (비로그인이면 memberId 가 null 이라 공개 글만)
    @Query("SELECT t FROM TripPlanEntity t LEFT JOIN t.member m"
            + " WHERE t.tripplanIsPublic IS NULL OR t.tripplanIsPublic = true OR m.memberId = :memberId")
    Page<TripPlanEntity> findVisible(@Param("memberId") Long memberId, Pageable pageable);

    // 공유(퍼가기) 수가 가장 높은 공개 템플릿 1건 조회
    @Query("SELECT t FROM TripPlanEntity t WHERE t.tripplanIsPublic IS NULL OR t.tripplanIsPublic = true"
            + " ORDER BY t.tripplanShareCount DESC, t.tripplanId DESC")
    List<TripPlanEntity> findTopByShareCount(Pageable pageable);

    // 최근 1달 안에서 퍼간(공유) 많은 순 (메인 추천 템플릿용). 작성일이 없는 예전 글은 빠진다
    @Query("SELECT t FROM TripPlanEntity t WHERE (t.tripplanIsPublic IS NULL OR t.tripplanIsPublic = true)"
            + " AND t.createdAt >= :since ORDER BY t.tripplanShareCount DESC, t.tripplanId DESC")
    List<TripPlanEntity> findTopSharedSince(@Param("since") LocalDateTime since, Pageable pageable);

    // 날짜 상관없이 최신순 공개 템플릿 (1달 안에 글이 없을 때 대체용)
    @Query("SELECT t FROM TripPlanEntity t WHERE t.tripplanIsPublic IS NULL OR t.tripplanIsPublic = true"
            + " ORDER BY t.tripplanId DESC")
    List<TripPlanEntity> findRecentPublic(Pageable pageable);
}