package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class CompanionPostEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "companion_post_id")
    private Long companionPostId;

    // 작성자
    @Column(nullable = false)
    private String companionPostMember;

    // 제목
    @Column(nullable = false, length = 200)
    private String companionPostTitle;

    // 내용
    @Column(columnDefinition = "TEXT", nullable = false)
    private String companionPostContent;

    // 지역 / 장소 (상세보기 화면 반영)
    @Column(nullable = false, length = 100)
    private String companionPostLocation;

    // 여행 시작일
    @Column(nullable = false)
    private LocalDate companionPostStartDate;

    // 여행 종료일
    @Column(nullable = false)
    private LocalDate companionPostEndDate;

    // 모집 인원 (숫자 타입 권장)
    @Column(nullable = false)
    private Integer companionPostPeople;

    // 태그 FK
    //@Column(name = "hash_tag_id")
   // private Long hashTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;


}