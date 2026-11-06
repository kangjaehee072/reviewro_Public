package com.reviewro.repository;

import com.reviewro.entity.LocationBookMarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationBookMarkRepository extends JpaRepository<LocationBookMarkEntity, Long> {


    // 특정 장소가 이미 북마크 되어 있는지 확인용
    boolean existsByLocation_LocationIdAndMember_MemberId(Long locationId,  Long memberId);

    // 특정 장소의 북마크 정보 찾기 (삭제할 때 사용)
    Optional<LocationBookMarkEntity> findByLocation_LocationIdAndMember_MemberId(Long locationId,  Long memberId);

    // 전체 북마크 목록 조회 (최신순 등)
    @Query("SELECT b FROM LocationBookMarkEntity b JOIN FETCH b.location ORDER BY b.createdAt DESC")
    List<LocationBookMarkEntity> findAllByOrderByCreatedAtDescAndMember_MemberId(Long memberId);

    //사용자 북마크 목록 조회
    @Query("SELECT b FROM LocationBookMarkEntity b JOIN FETCH b.location WHERE b.member.memberId = :memberId ORDER BY b.createdAt DESC")
    List<LocationBookMarkEntity> findBookmarksByMemberId(@Param("memberId") Long memberId);



    // 회원 탈퇴: 저장한 장소를 모두 지운다
    @Modifying
    @Query("DELETE FROM LocationBookMarkEntity b WHERE b.member.memberId = :memberId")
    void deleteAllOfMember(@Param("memberId") Long memberId);
}
