package com.reviewro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table (name = "schedule_plans")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SchedulePlansEntity extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_id")
    private DailyPlansEntity dailyPlansEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    private Integer scheduleNumber;
    private String scheduleStartTime;
    private String scheduleEndTime;
    private String scheduleDescription;
    private Integer scheduleRunTime;
    private Integer scheduleDistance;


}
