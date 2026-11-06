package com.reviewro.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class TravelPlansDTO {

    private Long planId;
    private Long memberId;
    private String planTitle;
    private String planStartDate;
    private String planEndDate;
    private String planMember;
    private String planType;
    private String planDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    List<DailyPlansDTO> dailyPlans;
    List<SchedulePlansDTO> schedulePlans;

}
