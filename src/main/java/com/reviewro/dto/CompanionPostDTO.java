package com.reviewro.dto;

import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class CompanionPostDTO {

    private Long companionPostId;             // PK
    private String companionPostMember;       // 작성자
    private String companionPostTitle;        // 제목
    private String companionPostContent;      // 내용
    private String companionPostLocation;     // 지역 / 장소
    private LocalDate companionPostStartDate; // 여행 시작일
    private LocalDate companionPostEndDate;   // 여행 종료일
    private Integer companionPostPeople;      // 모집 인원
    private LocalDateTime createdAt;          // 작성일시 (BaseTimeEntity)
    private LocalDateTime updatedAt;
    // 수정일시 (BaseTimeEntity)

    private Long CompanionPostMemberId;
    private String thumbnail;

}