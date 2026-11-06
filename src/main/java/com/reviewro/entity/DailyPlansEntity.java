package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "daily_plans")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class DailyPlansEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dailyId;

    @ManyToOne (fetch = FetchType.LAZY)
    private TravelPlansEntity  travelPlan;

    private Integer dailyNumber;
    private LocalDate dailyDate;

    @OneToMany(mappedBy ="dailyPlansEntity",cascade = CascadeType.ALL, orphanRemoval = true )
    @OrderBy ("scheduleNumber")
    private List<SchedulePlansEntity> schedulePlans = new ArrayList<>();

}
