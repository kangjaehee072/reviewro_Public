package com.reviewro.repository;

import com.reviewro.entity.LocationImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LocationImageRepository extends JpaRepository<LocationImageEntity, Long> {
    // 이 장소의 사진들. 상세 모달에서 좌우로 넘겨 본다
    List<LocationImageEntity> findByLocation_LocationIdOrderByImageIdAsc(Long locationId);

    // 목록용: 여러 장소의 사진을 한 번에 (장소마다 따로 조회하지 않는다)
    @Query("SELECT i FROM LocationImageEntity i WHERE i.location.locationId IN :ids ORDER BY i.imageId")
    List<LocationImageEntity> findByLocationIds(@Param("ids") Collection<Long> ids);
}
