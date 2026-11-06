package com.reviewro.repository;

import com.reviewro.entity.GameVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameVoteRepository extends JpaRepository<GameVoteEntity, Long> {

    // 특정 게임에 특정 회원이 투표한 내역이 있는지 확인
    Optional<GameVoteEntity> findByBalanceGame_BalanceGameIdAndMember_MemberId(Long balanceGameId, Long memberId);
}
