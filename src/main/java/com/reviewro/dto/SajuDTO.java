package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SajuDTO {

    private Long sajuId;
    private String sajuName;
    private String sajuGender;
    private String sajuBirthDate;
    private String sajuBirthTime;
    private Boolean sajuLunar;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
