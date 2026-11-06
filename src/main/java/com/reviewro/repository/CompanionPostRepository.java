package com.reviewro.repository;

import com.reviewro.entity.CompanionPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;

public interface CompanionPostRepository extends JpaRepository<CompanionPostEntity, Long> {

    List<CompanionPostEntity> findAllByOrderByCompanionPostIdDesc();

    @Query(value = "SELECT c FROM CompanionPostEntity c LEFT JOIN FETCH c.member",
            countQuery = "SELECT COUNT(c) FROM CompanionPostEntity c")
    Page<CompanionPostEntity> findPageWithMember(Pageable pageable);

    Page<CompanionPostEntity> findByCompanionPostTitleContaining(String keyword, Pageable pageable);


    //여행 시작일이 조회일 이후면서 가장 빠른 일자 ASC 추출
    @Query("SELECT c FROM CompanionPostEntity c WHERE c.companionPostStartDate >= CURRENT_DATE ORDER BY c.companionPostStartDate ASC, c.companionPostId DESC")
    List<CompanionPostEntity> findTopByUpcoming(Pageable pageable);


}
