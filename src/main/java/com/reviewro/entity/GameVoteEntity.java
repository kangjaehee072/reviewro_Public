package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name="game_vote",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_game_vote",
                columnNames = {"balance_game_id", "member_id"}))
@NoArgsConstructor
public class GameVoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gameVoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "balance_game_id", nullable = false)
    private BalanceGameEntity balanceGame;

    // 문자열 ID 대신 MemberEntity와 직접 연동
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(name = "game_vote_selected_option", length = 1, nullable = false)
    private String gameVoteSelectedOption;
}
