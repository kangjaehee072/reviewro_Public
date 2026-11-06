package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 여행 계획 하나에 저장한 AI 가이드 (계획당 하나).
// AI 가 쓴 문장(JSON)만 저장한다. 운영시간·휴무 같은 사실은 볼 때마다 장소 DB 에서 새로 읽는다
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "travel_guide", uniqueConstraints = @UniqueConstraint(name = "uk_travel_guide_plan", columnNames = "plan_id"))
public class TravelGuideEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guideId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Long memberId;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;
}
