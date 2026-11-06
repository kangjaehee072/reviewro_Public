package com.reviewro.service;

import com.reviewro.ai.AiSearchExtractor;
import com.reviewro.dto.PlaceHitDTO;
import com.reviewro.entity.Address;
import com.reviewro.entity.CategoryEntity;
import com.reviewro.entity.TypeEntity;
import com.reviewro.repository.CategoryRepository;
import com.reviewro.repository.LocationSearchRepository;
import com.reviewro.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceSearchService {

    private final LocationSearchRepository locationSearchRepository;
    private final TypeRepository typeRepository;
    private final CategoryRepository categoryRepository;

    // AI 가 만든 조건으로 검색. 이름으로 받는 이유는 AI 가 id 를 모르기 때문이다
    // city(시·군·구)가 있으면 주소에 그 이름이 들어간 곳만 찾는다.
    // 모델이 시/도를 잘못 짚어도(서산인데 충북 등) 시·군 이름으로 다시 찾아 본다
    public List<PlaceHitDTO> searchForAi(Address area, String city, String typeName, List<String> categoryNames,
                                         String keyword, int fetchLimit, int sendLimit) {
        String cityName = city == null || city.isBlank() ? null : AiSearchExtractor.stem(city);
        if (area == null && cityName == null) {
            throw new IllegalArgumentException("지역을 지정해 주세요.");
        }

        List<PlaceHitDTO> found = search(area, cityName, typeName, categoryNames, keyword, fetchLimit);
        if (found.isEmpty() && area != null && cityName != null) {
            found = search(null, cityName, typeName, categoryNames, keyword, fetchLimit);
        }
        return shuffleWithinHits(found, sendLimit);
    }

    private List<PlaceHitDTO> search(Address area, String cityName, String typeName, List<String> categoryNames,
                                     String keyword, int fetchLimit) {
        Long typeId = findTypeId(typeName);

        return locationSearchRepository.searchForAi(
                area == null ? "" : area.getShortName(),
                area == null ? "" : area.getFullName(),
                cityName == null ? "" : cityName,
                typeId,
                toCategoryIds(categoryNames),
                keyword == null ? "" : keyword.trim(),
                fetchLimit);
    }

    // 사람이 흔히 쓰는 말 -> 등록된 업종 이름.
    // 모델에게 업종 목록을 enum 으로 줘도 "맛집" 처럼 제 말로 바꿔 보낼 때가 있다
    private static final String[][] TYPE_WORDS = {
            {"맛집", "음식점"}, {"식당", "음식점"}, {"밥집", "음식점"}, {"먹거리", "음식점"},
            {"숙소", "숙박"}, {"호텔", "숙박"}, {"펜션", "숙박"}, {"게스트하우스", "숙박"},
            {"관광지", "자연/관광명소"}, {"명소", "자연/관광명소"}, {"여행지", "자연/관광명소"},
            {"공원", "자연/관광명소"}, {"자연", "자연/관광명소"},
            {"전시", "문화/예술"}, {"박물관", "문화/예술"}, {"미술관", "문화/예술"},
            {"레저", "액티비티/체험"}, {"놀거리", "액티비티/체험"},
            {"축제", "축제/행사"}, {"행사", "축제/행사"},
            {"시장", "쇼핑"}, {"쇼핑몰", "쇼핑"}
    };

    // 업종 이름을 id 로 바꾼다.
    // 이름이 딱 안 맞으면 조건이 통째로 사라져서 엉뚱한 업종이 섞여 나온다.
    // (모델이 "카페" 를 보내면 "카페/디저트" 와 안 맞아 업종 필터가 없는 검색이 된다)
    // 그래서 정확히 맞는 것을 먼저 찾고, 없으면 한쪽이 다른 쪽을 품은 것까지 본다
    private Long findTypeId(String typeName) {
        String want = blankToNull(typeName);
        if (want == null) {
            return null;
        }
        want = want.trim();

        // 흔한 말이면 등록된 이름으로 먼저 바꿔 놓는다
        for (String[] pair : TYPE_WORDS) {
            if (pair[0].equals(want)) {
                want = pair[1];
                break;
            }
        }

        List<TypeEntity> types = typeRepository.findAll();

        for (TypeEntity t : types) {
            if (t.getTypeName().equals(want)) {
                return t.getTypeId();
            }
        }
        // "카페" -> "카페/디저트", "체험" -> "액티비티/체험"
        for (TypeEntity t : types) {
            if (t.getTypeName().contains(want) || want.contains(t.getTypeName())) {
                return t.getTypeId();
            }
        }
        return null;
    }

    // 가점이 같은 것끼리만 섞는다. 가점 순서는 지키므로 잘 맞는 게 여전히 먼저 온다.
    // 섞는 이유: 같은 질문에 늘 같은 답이 나오면 추천으로서 의미가 없다.
    // ORDER BY RAND() 를 안 쓰는 이유: 걸러진 행을 전부 정렬해야 해서 비싸다.
    private List<PlaceHitDTO> shuffleWithinHits(List<PlaceHitDTO> found, int sendLimit) {
        Map<Integer, List<PlaceHitDTO>> byHits = new TreeMap<>(Collections.reverseOrder());
        for (PlaceHitDTO p : found) {
            int hits = p.getHits() == null ? 0 : p.getHits();
            byHits.computeIfAbsent(hits, k -> new ArrayList<>()).add(p);
        }

        List<PlaceHitDTO> result = new ArrayList<>();
        for (List<PlaceHitDTO> group : byHits.values()) {
            Collections.shuffle(group);
            for (PlaceHitDTO p : group) {
                if (result.size() >= sendLimit) return result;
                result.add(p);
            }
        }
        return result;
    }

    // 이름을 id 로 바꾼다. IN () 는 빈 목록을 못 받으므로 없을 때는 맞지 않는 값을 하나 넣는다
    private List<Long> toCategoryIds(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return List.of(-1L);
        }
        List<Long> ids = new ArrayList<>();
        for (CategoryEntity c : categoryRepository.findByCategoryNameIn(categoryNames)) {
            ids.add(c.getCategoryId());
        }
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    // 조건없음용 방어코드
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
