package com.reviewro.service;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.LocationDTO;
import com.reviewro.repository.SajuPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SajuPlaceService {

    private static final int LIMIT = 4;

    // 부족한 오행 → 그 기운을 채워 주는 장소의 태그 키워드.
    // location_element 컬럼에는 오행 값이 아니라 해시태그가 들어 있어서 태그로 고른다
    private static final Map<String, String> KEYWORDS = Map.of(
            "wood", "숲|공원|산책|자연|정원|꽃|대나무|국립공원|등산|생태|힐링",
            "fire", "축제|야경|액티비티|체험|테마파크|놀이|공연|일출|루지|카트",
            "earth", "전통|한옥|마을|시장|한식|한정식|박물관|역사",
            "metal", "사찰|궁궐|유네스코|전망대|돌담|석굴암|해인사|국보",
            "water", "바다|해변|호수|하천|오션뷰|해안|낙조|계곡|물회"
    );

    private final SajuPlaceRepository sajuPlaceRepository;

    // 부족한 오행에 어울리는 장소 몇 곳 (없으면 빈 목록)
    public List<LocationDTO> findForElement(String element) {
        String pattern = KEYWORDS.get(element);
        if (pattern == null) {
            return List.of();
        }
        return sajuPlaceRepository.findByKeywords(pattern, LIMIT).stream()
                .map(e -> AutoMapper.map(e, LocationDTO.class))
                .toList();
    }
}
