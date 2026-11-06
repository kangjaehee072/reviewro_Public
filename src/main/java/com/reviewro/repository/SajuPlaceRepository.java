package com.reviewro.repository;

import com.reviewro.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 사주 페이지 전용 장소 조회 (기존 LocationRepository 는 건드리지 않는다)
public interface SajuPlaceRepository extends JpaRepository<LocationEntity, Long> {

    // 태그·이름·설명 중 하나라도 키워드(정규식 a|b|c)에 걸리는 장소를 무작위로.
    // 장소가 100곳 남짓이라 ORDER BY RAND() 로 충분하다
    @Query(value = """
            SELECT * FROM location
            WHERE location_element REGEXP :pattern
               OR location_name REGEXP :pattern
               OR location_description REGEXP :pattern
            ORDER BY RAND()
            LIMIT :limit
            """, nativeQuery = true)
    List<LocationEntity> findByKeywords(@Param("pattern") String pattern, @Param("limit") int limit);
}
