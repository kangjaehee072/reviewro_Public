package com.reviewro.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
public class DailyPlansDTO {

    private Long dailyId;
    private Integer dailyNumber;
    private LocalDate dailyDate;


}
