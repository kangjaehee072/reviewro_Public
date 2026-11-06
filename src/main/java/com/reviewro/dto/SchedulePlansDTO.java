package com.reviewro.dto;

import com.reviewro.entity.SchedulePlansEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SchedulePlansDTO {

    private Long scheduleId;
    private Long dailyId;
    private Long locationId;
    private Integer scheduleNumber;
    private String scheduleStartTime;
    private String scheduleEndTime;
    private String scheduleDescription;
    private Integer scheduleRunTime;
    private Integer scheduleDistance;
    private LocationDTO location;


}
