package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BalanceGameCommentDTO {

    private Long commentId;        // 댓글 PK
    private Long balanceGameId;    // 속한 밸런스 게임 ID
    private Long memberId;         // 작성자 회원 ID (선택: 삭제 권한 확인용)
    private String writer;         // 작성자 닉네임 (서버에서 채움)
    private String content;        // 댓글 내용
}