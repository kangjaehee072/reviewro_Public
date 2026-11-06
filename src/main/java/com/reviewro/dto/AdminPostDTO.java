package com.reviewro.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AdminPostDTO {

    private Long adminPostId;             // PK
    private Long adminPostMemberId;       // 작성자 (FK)
    private String adminPostCategory;     // 카테고리(공지/추천)
    private String adminPostTitle;        // 제목
    private String adminPostContent;      // 내용
    private String adminPostImage;        // 사진
    private Long adminPostViewCount;      // 조회수
    private Boolean adminPostPinned;      // 상단고정여부 (엔티티와 같은 이름이어야 AutoMapper 가 복사한다)
    private LocalDateTime createdAt;      // 작성일시
    private LocalDateTime updatedAt;      // 수정일시
    private String thumbnail;
}
