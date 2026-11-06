package com.reviewro.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentDTO {
    private Long commentId;
    private Long parentId;
    private String writer;        // 닉네임
    private String content;
    private LocalDateTime createdAt;
    private boolean deleted;
    private boolean mine;         // 내가 쓴 글인가 (삭제 버튼 노출용)
}
