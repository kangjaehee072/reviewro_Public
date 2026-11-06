package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameVoteDTO {

    private Long gameVoteId;            // 투표 PK

    private Long balanceGameId;         // 어떤 밸런스 게임인지 (FK 연결용)

    private Long memberId;              // 투표한 회원 번호

    private String gameVoteSelectedOption; // 선택한 옵션 (예: "A" 또는 "B")
}
