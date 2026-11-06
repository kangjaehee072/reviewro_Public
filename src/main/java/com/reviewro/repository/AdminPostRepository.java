package com.reviewro.repository;

import com.reviewro.entity.AdminPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminPostRepository extends JpaRepository<AdminPostEntity, Long> {

    String NOTICES = """
       SELECT p FROM AdminPostEntity p
       WHERE p.adminPostCategory = :category
       ORDER BY COALESCE(p.adminPostPinned, false) DESC,
                p.adminPostId DESC
       """;

    @Query(NOTICES)
    Page<AdminPostEntity> findNotices(@Param("category") String category, Pageable pageable);

    // 메인 화면용 최신 1건. List 로 받으면 COUNT 쿼리 없이 LIMIT 만 붙는다
    @Query(NOTICES)
    List<AdminPostEntity> findNoticeList(@Param("category") String category, Pageable pageable);

    @Query(value = "SELECT p FROM AdminPostEntity p LEFT JOIN FETCH p.member",
            countQuery = "SELECT COUNT(p) FROM AdminPostEntity p")
    Page<AdminPostEntity> findPageWithMember(Pageable pageable);

    Page<AdminPostEntity> findByAdminPostTitleContaining(String keyword, Pageable pageable);



}