package com.reviewro.dto;

// AI 검색 결과 한 줄. hits 는 조건이 몇 개 맞았는지를 나타내는 가점이다.
// 네이티브 쿼리의 인터페이스 투영으로 채워진다 (getter 이름 = SELECT 별칭)
public interface PlaceHitDTO {

    Long getLocationId();

    String getLocationName();

    String getLocationAddress();

    String getLocationDescription();

    Double getLocationLat();

    Double getLocationLng();

    Double getLocationReviewAvg();

    String getTypeName();

    // 이 장소에 붙은 카테고리 전부 (쉼표 구분)
    String getAllCategories();

    // 그중 AI 가 요청한 조건에 맞은 것만 (쉼표 구분). 비어있으면 조건에 안 맞은 장소다
    String getMatchedCategories();

    // 맞은 조건 개수. 카테고리 매칭 수 + 키워드 매칭 여부(0/1)
    Integer getHits();
}
