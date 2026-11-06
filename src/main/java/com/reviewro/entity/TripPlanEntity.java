package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Entity
@Getter
@Setter
@Table(name = "trip_plan")
@NoArgsConstructor
// 작성일(createdAt)이 생긴다. 메인 '추천 템플릿' 을 최근 1달 기준으로 뽑으려고 둔다.
// 기존 행은 작성일이 비어 있어 1달 조건에서 빠지고, 그때는 최근 글로 대체한다
public class TripPlanEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tripplanId;

    @Column(nullable = false)
    private String tripplanTitle;

    @Column(nullable = false)
    private String tripplanContent;

    private Integer tripplanShareCount=0;

    private Boolean tripplanIsPublic = true;

    @OneToMany(mappedBy = "tripPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripDayEntity> tripDays = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private MemberEntity member;
}
