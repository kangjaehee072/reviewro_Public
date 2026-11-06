package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ReviewEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;             // PK (long)

    private String reviewTitle;        // 제목

    private String reviewUser;

    private LocalDate reviewDate;      // 작성일

    private String reviewArea;         // 지역

    private String reviewLocation;     // 장소

    private String reviewCategory;     // 카테고리

    private LocalDate reviewStartDate; // 여행시작일

    private LocalDate reviewEndDate;   // 여행종료일

    private Integer reviewPeopleCount;  // 인원수

    @Column(length = 2000)
    private String reviewContent;      // 내용

    private String reviewVisitPurpose; // 방문 목적

    private String reviewCompanionType;// 동행자 유형

    private Double reviewStarRating;   // 별점

    // 작성자 연결 (MemberEntity 매핑)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    //private String reviewHashTag;      // 태그

    //private String reviewImage;        // 사진 첨부

    // 1:N 관계 설정 (리뷰 삭제 시 연관된 이미지/태그도 함께 삭제되도록 cascade 설정)
   // @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
   // private List<ReviewImageEntity> images = new ArrayList<>();

    // @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<ReviewHashTagEntity> hashTags = new ArrayList<>();
}