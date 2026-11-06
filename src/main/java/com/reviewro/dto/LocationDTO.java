package com.reviewro.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class LocationDTO {

    private Long locationId;
    private String locationName;
    private Double locationLng;
    private Double locationLat;
    private String locationDescription;
    private String locationAddress;
    private String locationAddressTw;
    private String locationNumber;
    private String locationDayofweek;
    private String locationDayoff;
    private String locationOpentime;
    private String locationClosetime;
    private String locationURL;
    private Double locationReviewAvg;
    private String locationCreatedBy;
    private String locationUpdatedBy;
    private String locationElement;
    private Long typeId;

    private List<Long> categories;


    private List<String> categorname;

    // 이 장소의 사진 주소들. 상세 모달에서 좌우로 넘겨 본다
    private List<String> images;

    // 목록에서 보여 줄 대표 사진 (첫 번째 사진, 없으면 null)
    private String thumbnail;
}
