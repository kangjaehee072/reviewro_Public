package com.reviewro.repository;

import com.reviewro.entity.ReactionEntity;
import com.reviewro.entity.ReactionKind;
import com.reviewro.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<ReactionEntity, Long> {

    // 내가 이 글에 누른 게 있나
    Optional<ReactionEntity> findByTargetTypeAndTargetIdAndMember_MemberId(
            TargetType targetType, Long targetId, Long memberId);

    // 이 글의 좋아요(또는 싫어요) 개수
    long countByTargetTypeAndTargetIdAndKind(
            TargetType targetType, Long targetId, ReactionKind kind);

    @Modifying
    @Query("delete from ReactionEntity r " +
            " where r.targetType = :type and r.targetId = :id")
    void deleteByTarget(@Param("type") TargetType type, @Param("id") Long id);

    // 여러 글의 좋아요 수를 한 번에 센다 (목록에서 글마다 따로 세면 글 수만큼 쿼리가 나간다)
    @Query("select r.targetId, count(r) from ReactionEntity r " +
            " where r.targetType = :type and r.kind = :kind and r.targetId in :ids " +
            " group by r.targetId")
    List<Object[]> countByTargetIds(@Param("type") TargetType type,
                                    @Param("kind") ReactionKind kind,
                                    @Param("ids") List<Long> ids);
}
