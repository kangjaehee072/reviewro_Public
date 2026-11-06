package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.LocationDTO;
import com.reviewro.entity.CategoryEntity;
import com.reviewro.entity.LocationEntity;
import com.reviewro.entity.TypeEntity;
import com.reviewro.entity.LocationImageEntity;
import com.reviewro.repository.CategoryRepository;
import com.reviewro.repository.LocationImageRepository;
import com.reviewro.repository.LocationRepository;
import com.reviewro.repository.MemberRepository;
import com.reviewro.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
@Service
public class LocationService {
    private final LocationRepository locationRepository;
    private final MemberRepository memberRepository;
    private final TypeRepository typeRepository;
    private final CategoryRepository categoryRepository;
    private final LocationImageRepository locationImageRepository;

    //insert
    public Long locationinsert(Long memberID, LocationDTO locationDTO) {
       checkUrl(locationDTO.getLocationURL());
       locationDTO.setLocationReviewAvg(null);
       LocationEntity locations = AutoMapper.map(locationDTO, LocationEntity.class);

       TypeEntity type = typeRepository.findById(locationDTO.getTypeId())
               .orElseThrow(() -> new IllegalArgumentException("타입을 찾을 수 없습니다."));
        locations.setType(type);

        List<CategoryEntity>  categoryList =  new ArrayList<>();
        for (Long categoryId : locationDTO.getCategories()) {
            CategoryEntity categoryEntity = categoryRepository.findById(categoryId)
                    .orElseThrow( () -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
            categoryList.add(categoryEntity);
        }
        locations.setCategories(categoryList);

       locations.setMember(memberRepository.findById(memberID).orElseThrow(()
               -> new IllegalArgumentException("존재하지 않는 회원입니다.")));
        locations = locationRepository.save(locations);
        return locations.getLocationId();
    }

    //list
    public List<LocationDTO> locationslist() {
        List<LocationEntity> locationEntities = locationRepository.findAll();
        List<LocationDTO> locations = new ArrayList<>();
        for (LocationEntity locationEntity : locationEntities) {
            locations.add(AutoMapper.map(locationEntity, LocationDTO.class));
        }
        return locations;
    }

    //read
    public LocationDTO locationread(Long id) {
        LocationEntity locationEntity = locationRepository.findById(id).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 장소입니다."));
        LocationDTO dto = AutoMapper.map(locationEntity, LocationDTO.class);

        List<String> images = new ArrayList<>();
        for (LocationImageEntity image : locationImageRepository.findByLocation_LocationIdOrderByImageIdAsc(id)) {
            images.add(image.getImagePath());
        }
        dto.setImages(images);

        return dto;
    }

    //update
    @Transactional
    public void locationupdate(LocationDTO locationDTO) {
        LocationEntity entity = locationRepository.findById(locationDTO.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소입니다."));
        checkUrl(locationDTO.getLocationURL());
        // 평균 평점은 리뷰로 계산되는 값이라 직접 바꾸지 못하게 한다
        locationDTO.setLocationReviewAvg(null);
        AutoMapper.copyNonNull(locationDTO, entity);
    }

    // 장소 링크는 화면에 href 로 걸린다. javascript: 같은 주소가 들어가지 않게 http(s) 만 받는다
    private void checkUrl(String url) {
        if (url != null && !url.isBlank()
                && !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("장소 링크는 http:// 또는 https:// 로 시작해야 합니다.");
        }
    }

    //delete
    public void locationdelete(Long id) {
        locationRepository.deleteById(id);
    }


}
