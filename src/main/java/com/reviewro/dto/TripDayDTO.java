package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TripDayDTO {

    private Long tripDayId;            // PK (수정/조회 시 필요)

    private String tripDayNumber;      // 예: "DAY 1", "DAY 2"

    // 해당 일자에 속한 장소 목록
    private List<TripPlaceDTO> tripPlaces = new ArrayList<>();

}
