package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripPlaceDTO {

    private Long tripPlaceId;          // PK (수정/조회 시 필요)

    private Long locationId;           // 장소 ID (FK)

    private String tripPlaceSequence;  // 방문 순서

    private String tripPlaceMemo;      // 장소별 메모
}
