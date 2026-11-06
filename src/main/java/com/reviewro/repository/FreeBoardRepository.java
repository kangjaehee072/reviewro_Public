package com.reviewro.repository;

import com.reviewro.entity.FreeBoardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FreeBoardRepository extends JpaRepository<FreeBoardEntity, Long> {
    List<FreeBoardEntity> findAllByOrderByFreeBoardIdDesc();

    @Query(value = "SELECT f FROM FreeBoardEntity f LEFT JOIN FETCH f.member",
            countQuery = "SELECT COUNT(f) FROM FreeBoardEntity f")
    Page<FreeBoardEntity> findPageWithMember(Pageable pageable);

    Page<FreeBoardEntity> findByfreeBoardTitleContaining(String keyword, Pageable pageable);

    @Query("SELECT r FROM FreeBoardEntity r ORDER BY r.freeBoardShareCount DESC")
    List<FreeBoardEntity> findTopByViewCount(Pageable pageable);

    // 최근 1달 안에서 공유 많은 순 (메인 핫토픽용). 작성일이 없는 예전 글은 빠진다
    @Query("SELECT r FROM FreeBoardEntity r WHERE r.createdAt >= :since ORDER BY r.freeBoardShareCount DESC, r.freeBoardId DESC")
    List<FreeBoardEntity> findHotSince(@Param("since") LocalDateTime since, Pageable pageable);

    // 날짜 상관없이 최신순 (1달 안에 글이 없을 때 대체용)
    @Query("SELECT r FROM FreeBoardEntity r ORDER BY r.freeBoardId DESC")
    List<FreeBoardEntity> findRecent(Pageable pageable);


}
