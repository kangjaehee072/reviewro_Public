package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TripPlanDTO {

    private Long tripplanId;           // PK

    private String tripplanTitle;      // 제목

    private String tripplanName;       // 작성자

    private String tripplanContent;    // 내용

    private Integer tripplanShareCount;// 공유수 (기본값 0)

    private Boolean tripplanIsPublic;  // 공개 여부

    private Long memberId;

    // 여행 일자별 목록
    private List<TripDayDTO> tripDays = new ArrayList<>();
}
