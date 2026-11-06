package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table (name = "travel_plans")
@NoArgsConstructor
public class TravelPlansEntity extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    private String planTitle;
    private String planStartDate;
    private String planEndDate;

    @Column (nullable = true)
    private String planMember;
    private String planType;
    private String planDescription;

    @OneToMany (mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyPlansEntity> dailyPlans = new ArrayList<>();

}
