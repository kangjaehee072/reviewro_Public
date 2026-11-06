package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BalanceGameDTO {

    private Long balanceGameId;         // 밸런스 게임 PK

    private String balanceGameTitle;    // 게임 제목 / 질문

    private String balanceGameOptionA;  // 옵션 A 내용

    private String balanceGameOptionB;  // 옵션 B 내용

    private Long balanceGameCountA = 0L;   // 옵션 A 투표 수

    private Long balanceGameCountB = 0L;   // 옵션 B 투표 수

    private Boolean balanceGameStatus = true;  // 게임 진행 상태

    private Long balanceGameMemberId;   // 작성자 회원 ID (엔티티의 member와 매핑용)

    private List<GameVoteDTO> gameVotes = new ArrayList<>();

    private List<BalanceGameCommentDTO> comments = new ArrayList<>();
}
