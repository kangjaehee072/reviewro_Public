package com.reviewro.service;


import com.reviewro.config.AutoMapper;
import com.reviewro.dto.LocationDTO;
import com.reviewro.dto.LocationSearchDTO;
import com.reviewro.dto.LocationSearchResultDTO;
import com.reviewro.entity.CategoryEntity;
import com.reviewro.entity.LocationEntity;
import com.reviewro.entity.TypeEntity;
import com.reviewro.repository.CategoryRepository;
import com.reviewro.repository.LocationSearchRepository;
import com.reviewro.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class LocationSearchService {

    private static final int MAX_RESULTS = 500;
    private final LocationSearchRepository locationSearchRepository;
    private final CategoryRepository categoryRepository;
    private final TypeRepository typeRepository;
    private final LocationThumbnails locationThumbnails;

    //관광지 조회 (최대 500개)
    public LocationSearchResultDTO search(LocationSearchDTO locationSearchDTO) {
        boolean hasAddress = locationSearchDTO.getAddress() != null;
        boolean hasCoordinate = locationSearchDTO.getLat() != null && locationSearchDTO.getLng() != null;
        boolean hasKeyword = locationSearchDTO.getKeyword() != null && !locationSearchDTO.getKeyword().isBlank();
        boolean hasType = locationSearchDTO.getTypeId() != null;
        boolean hasCategories = locationSearchDTO.getCategories() != null && !locationSearchDTO.getCategories().isEmpty();
        boolean hasElement = locationSearchDTO.getElement() != null;

        if (!hasAddress && !hasCoordinate && !hasKeyword && !hasType && !hasCategories && !hasElement) {
            return new LocationSearchResultDTO(List.of(), false);
        }

        TypeEntity type = null;
        if (locationSearchDTO.getTypeId() != null) {
            type = typeRepository.findById(locationSearchDTO.getTypeId())
                    .orElseThrow(() ->new IllegalArgumentException("타입을 찾을 수 없습니다."));
        }

        List<CategoryEntity> categories = List.of();
        if (locationSearchDTO.getCategories() != null && !locationSearchDTO.getCategories().isEmpty()){
            categories = categoryRepository.findAllById(locationSearchDTO.getCategories());
            if (categories.size() != locationSearchDTO.getCategories().size()) {
                throw new IllegalArgumentException("존재하지 않는 카테고리가 포함되어 있습니다.");
            }        }

        String shortName = hasAddress ? locationSearchDTO.getAddress().getShortName() : null;
        String fullName = hasAddress ? locationSearchDTO.getAddress().getFullName() : null;

        // 1. 페이지 번호 보정 (기본값 1) 및 제한 설정
        int page = (locationSearchDTO.getPage() > 0) ? locationSearchDTO.getPage() : 1;
        int limit = 10;
        int offset = (page - 1) * limit;
        int fetchLimit = limit + 1; // 다음 페이지 유무 확인을 위해 11개를 조회

        List<LocationEntity> found = locationSearchRepository.search(
                shortName,
                fullName,
                categories.isEmpty(),
                categories.isEmpty() ? List.of(-1L)
                        : categories.stream().map(CategoryEntity::getCategoryId).toList(),
                type != null ? type.getTypeId() : null,
                locationSearchDTO.getElement(),
                locationSearchDTO.getMinRating(),
                locationSearchDTO.getKeyword(),
                locationSearchDTO.getLat(),
                locationSearchDTO.getLng(),
                locationSearchDTO.getRadius(),
                offset,
                fetchLimit
        );

        // 3. 'more' 플래그 판단 로직
        boolean more = false;
        if (found.size() > limit) {
            more = true; // 10개를 초과해서 조회되었다면 다음 페이지가 있음
            found.remove(found.size() - 1); // 11번째 데이터는 확인용이므로 리스트에서 제거
        }

        // 4. Entity를 DTO로 변환
        List<LocationDTO> locations = found.stream()
                .map(e -> AutoMapper.map(e, LocationDTO.class))
                .toList();
        return new LocationSearchResultDTO(locationThumbnails.fill(locations), more);
    }

    private Sort sortOf (String sort) {
        if("rating".equals(sort))
            return Sort.by(Sort.Direction.DESC, "locationReviewAvg");
        if("name".equals(sort))
            return Sort.by("locationName");
        return Sort.by(Sort.Direction.DESC, "locationId"); //최신순
    }
}
