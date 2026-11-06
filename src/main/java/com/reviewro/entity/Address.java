package com.reviewro.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Address {

    SEOUL("서울", "서울특별시"),       BUSAN("부산", "부산광역시"),
    DAEGU("대구", "대구광역시"),       INCHEON("인천", "인천광역시"),
    GWANGJU("광주", "광주광역시"),     DAEJEON("대전", "대전광역시"),
    ULSAN("울산", "울산광역시"),       SEJONG("세종", "세종특별자치시"),
    GYEONGGI("경기", "경기도"),        GANGWON("강원", "강원특별자치도"),
    CHUNGBUK("충북", "충청북도"),      CHUNGNAM("충남", "충청남도"),
    JEONBUK("전북", "전라북도"),       JEONNAM("전남", "전라남도"),
    GYEONGBUK("경북", "경상북도"),     GYEONGNAM("경남", "경상남도"),
    JEJU("제주", "제주특별자치도");

    private final String shortName;
    private final String fullName;

    // 주소 문자열에서 시/도를 찾는다. 못 찾으면 null ("ㅁㄴㅇㄹ" 같은 값)
    // fullName 을 먼저 보는 이유: "충청북도" 가 "충북" 보다 길어서 긴 쪽부터 맞춰야 한다
    public static Address of(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String head = address.trim().split(" ")[0];
        for (Address a : values()) {
            if (head.startsWith(a.fullName) || head.startsWith(a.shortName)) {
                return a;
            }
        }
        return null;
    }
}
