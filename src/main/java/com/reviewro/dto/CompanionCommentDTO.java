package com.reviewro.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanionCommentDTO {

    private Long companionCommentId;          // 댓글 PK
    private Long companionPostId;             // 게시글 FK (게시글 아이디 매핑용)
    private String companionCommentMember;    // 댓글 작성자
    private String companionCommentContent;   // 댓글 내용
    private LocalDateTime createdAt;          // 작성일시 (BaseTimeEntity)
    private LocalDateTime updatedAt;          // 수정일시 (BaseTimeEntity)
}