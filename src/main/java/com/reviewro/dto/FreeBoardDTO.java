package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FreeBoardDTO {

    private Long freeBoardId;          // PK
    private long likeCount;            // 좋아요 수 (목록에서 채운다)

    private String freeBoardTitle;     // 제목

    private String freeBoardName;      // 작성자

    private LocalDate freeBoardDate;   // 작성일

    private String freeBoardContent;   // 내용

    private Integer freeBoardShareCount; // 공유수 (기본값 0)

    private Long freeBoardMemberId;      // 작성자 회원 번호 (수정/삭제 버튼 표시용)

    //private String hashTagName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String thumbnail;

}
