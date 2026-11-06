package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "admin_post")
public class AdminPostEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminPostId;             // PK (long)

    private Long adminPostMemberId;       // 작성자 (Long / FK)

    private String adminPostCategory;     // 카테고리(공지/추천)

    private String adminPostTitle;        // 제목

    @Column(columnDefinition = "TEXT")
    private String adminPostContent;      // 내용

    private String adminPostImage;        // 사진

    private Long adminPostViewCount;      // 조회수

    private Boolean adminPostPinned = false;    // 상단고정여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;

}