package com.reviewro.repository;

import com.reviewro.entity.ReviewEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    // 1. 단순 최신순 정렬 (작성자 조회를 안 하거나 별도로 처리할 때)
    List<ReviewEntity> findAllByOrderByReviewIdDesc();

    // 2. N+1 문제 방지: 목록에서 작성자(member) 정보를 함께 조인해서 최신순 조회할 때
    @Query("SELECT r FROM ReviewEntity r LEFT JOIN FETCH r.member ORDER BY r.reviewId DESC")
    List<ReviewEntity> findAllWithMemberOrderByReviewIdDesc();

    @Query(value = "SELECT r FROM ReviewEntity r LEFT JOIN FETCH r.member",
            countQuery = "SELECT COUNT(r) FROM ReviewEntity r")
    Page<ReviewEntity> findPageWithMember(Pageable pageable);

    Page<ReviewEntity> findByReviewTitleContaining(String keyword, Pageable pageable);


    //추천 높은 순 추출
    @Query("SELECT r FROM ReviewEntity r ORDER BY r.reviewStarRating DESC, r.reviewId DESC")
    List<ReviewEntity> findTopByBestReview(Pageable pageable);

    // 최근 1달 안에서 별점 높은 순 (메인 베스트 평점용). 작성일이 없는 예전 글은 빠진다
    @Query("SELECT r FROM ReviewEntity r WHERE r.createdAt >= :since ORDER BY r.reviewStarRating DESC, r.reviewId DESC")
    List<ReviewEntity> findBestSince(@Param("since") LocalDateTime since, Pageable pageable);

    // 날짜 상관없이 최신순 (1달 안에 글이 없을 때 대체용)
    @Query("SELECT r FROM ReviewEntity r ORDER BY r.reviewId DESC")
    List<ReviewEntity> findRecent(Pageable pageable);


}