package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

// 그룹에 공유한 여행계획. 계획 자체는 공유한 사람 것이고, 그룹원은 읽기만 한다
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "travel_group_plan",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "plan_id"}))
public class TravelGroupPlanEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private TravelGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TravelPlansEntity plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by", nullable = false)
    private MemberEntity sharedBy;
}
