package com.reviewro.repository;

import com.reviewro.entity.TravelGroupMemberEntity;
import com.reviewro.entity.TravelGroupMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelGroupMemberRepository extends JpaRepository<TravelGroupMemberEntity, Long> {

    Optional<TravelGroupMemberEntity> findByGroup_GroupIdAndMember_MemberId(Long groupId, Long memberId);

    // 내가 속한(또는 초대받은) 그룹 목록
    @Query("SELECT gm FROM TravelGroupMemberEntity gm JOIN FETCH gm.group g JOIN FETCH g.owner"
            + " LEFT JOIN FETCH gm.invitedBy WHERE gm.member.memberId = :memberId AND gm.status = :status"
            + " ORDER BY gm.groupMemberId DESC")
    List<TravelGroupMemberEntity> findMine(@Param("memberId") Long memberId,
                                           @Param("status") TravelGroupMemberStatus status);

    // 그룹 구성원 목록 (들어온 순)
    @Query("SELECT gm FROM TravelGroupMemberEntity gm JOIN FETCH gm.member"
            + " WHERE gm.group.groupId = :groupId ORDER BY gm.groupMemberId")
    List<TravelGroupMemberEntity> findByGroup(@Param("groupId") Long groupId);

    long countByGroup_GroupIdAndStatus(Long groupId, TravelGroupMemberStatus status);

    void deleteByGroup_GroupId(Long groupId);
}
