package com.reviewro.service;

import com.reviewro.dto.LocationDTO;
import com.reviewro.entity.LocationImageEntity;
import com.reviewro.repository.LocationImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 장소 목록(관광지 찾기·저장한 장소·관리자 목록)에 대표 사진 한 장씩 붙인다
@Component
@RequiredArgsConstructor
public class LocationThumbnails {

    private final LocationImageRepository locationImageRepository;

    public List<LocationDTO> fill(List<LocationDTO> locations) {
        if (locations.isEmpty()) {
            return locations;
        }
        List<Long> ids = locations.stream().map(LocationDTO::getLocationId).toList();
        // 사진 번호가 작은 것이 먼저 오므로 장소마다 처음 만나는 사진이 대표 사진
        Map<Long, String> first = new HashMap<>();
        for (LocationImageEntity image : locationImageRepository.findByLocationIds(ids)) {
            first.putIfAbsent(image.getLocation().getLocationId(), image.getImagePath());
        }
        locations.forEach(loc -> loc.setThumbnail(first.get(loc.getLocationId())));
        return locations;
    }
}
