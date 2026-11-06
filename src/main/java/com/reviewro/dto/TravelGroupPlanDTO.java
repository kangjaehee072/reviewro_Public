package com.reviewro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelGroupPlanDTO {
    private Long planId;
    private String planTitle;
    private String planStartDate;
    private String planEndDate;
    private Long sharedById;
    private String sharedByNickname;
}
