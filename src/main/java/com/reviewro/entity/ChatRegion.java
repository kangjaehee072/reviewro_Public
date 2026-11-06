package com.reviewro.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 톡방을 묶는 큰 지역 (광역 시·도). 화면 순서도 이 순서를 따른다
@Getter
@RequiredArgsConstructor
public enum ChatRegion {
    SEOUL("서울"),
    GYEONGGI("경기"),
    INCHEON("인천"),
    GANGWON("강원"),
    CHUNGBUK("충북"),
    CHUNGNAM("충남"),
    DAEJEON("대전"),
    SEJONG("세종"),
    JEONBUK("전북"),
    JEONNAM("전남"),
    GWANGJU("광주"),
    GYEONGBUK("경북"),
    GYEONGNAM("경남"),
    DAEGU("대구"),
    ULSAN("울산"),
    BUSAN("부산"),
    JEJU("제주");

    private final String label;
}
