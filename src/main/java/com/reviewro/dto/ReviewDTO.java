package com.reviewro.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {

    private Long reviewId;              // PK
    private long likeCount;            // 좋아요 수 (목록에서 채운다)
    private String reviewTitle;         // 제목
    private String reviewUser;          // 작성자 (서버가 채운다. 폼에서 받지 않는다)
    private LocalDate reviewDate;       // 작성일
    private String reviewArea;          // 지역
    private String reviewLocation;      // 장소
    private String reviewCategory;      // 카테고리
    private LocalDate reviewStartDate;  // 여행시작일
    private LocalDate reviewEndDate;    // 여행종료일
    private Integer reviewPeopleCount;   // 인원수
    private String reviewContent;       // 내용
    private String reviewVisitPurpose;  // 방문 목적
    private String reviewCompanionType; // 동행자 유형
    private Double reviewStarRating;    // 별점

    private Long reviewMemberId;        // 작성자 회원 번호 (수정/삭제 버튼 표시 및 권한 확인용)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;    // 수정일

    private String Thumbnail;

    //private String reviewHashTag;       // 태그
    //private String reviewImage;         // 사진 첨부
}