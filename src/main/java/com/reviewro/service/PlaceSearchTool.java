package com.reviewro.service;

import com.reviewro.ai.AiTool;
import com.reviewro.dto.PlaceHitDTO;
import com.reviewro.entity.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 파트라슈가 쓰는 장소 검색 도구.
// AI 가 대화에서 지역·타입·카테고리·키워드를 뽑아 호출하면 DB 를 찾아 돌려준다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceSearchTool implements AiTool {

    // AI 에게 보낼 장소 수. 프롬프트 토큰이 응답 속도를 지배하므로 적게 준다
    private static final int SEND_LIMIT = 8;

    // DB 에서 받아올 수. 이 안에서 섞어서 SEND_LIMIT 개를 고른다 (같은 질문에 같은 답만 나오지 않게)
    private static final int FETCH_LIMIT = 20;

    // 검색 결과가 0곳일 때 대화 기록에 남길 문장 (다음 턴에 모델이 읽는다)
    private static final String NOT_FOUND = "그 지역에는 아직 등록된 장소가 없습니다.";
    // 시·군으로 찾았는데 0곳일 때 ("서산" + 이 문장)
    private static final String NOT_FOUND_IN_CITY = "에는 아직 등록된 장소가 없습니다.";

    // 0곳일 때 모델 대신 사용자에게 바로 보내는 답
    private static final String NOT_FOUND_REPLY =
            "아쉽지만 그 지역에서는 추천할 만한 장소를 찾지 못했어요 😢\n"
            + "다른 지역을 알려주시거나, 원하는 분위기를 조금 넓혀서 말씀해 주시면 다시 찾아볼게요!";

    private final PlaceSearchService placeSearchService;
    private final AiVocab aiVocab;
    private final JsonMapper jsonMapper;

    @Override
    public String name() {
        return "search_places";
    }

    // 선택지를 DB 에서 만들어 넣는다. 관리자가 타입·카테고리를 추가하면 AI 가 즉시 알게 된다
    @Override
    public Map<String, Object> schema() {
        AiVocab.Vocab vocab = aiVocab.load();

        Map<String, Object> props = new LinkedHashMap<>();
        // 영문 코드(SEOUL)가 아니라 한글을 넣는다.
        // 모델이 "어느 지역이세요?" 하고 되물을 때 이 목록을 그대로 읽어주기 때문이다.
        // 한글로 와도 parseArea 의 Address.of 가 받아준다
        props.put("city", Map.of(
                "type", "string",
                "description", "사용자가 말한 시·군·구 이름 (예: 서산, 해운대, 전주). 시/도만 말했으면 생략"));
        props.put("area", Map.of(
                "type", "string",
                "description", "시/도. city 가 있으면 그 지역이 속한 시/도. 사용자가 지역을 밝히지 않았으면 이 도구를 부르지 말고 먼저 물어본다",
                "enum", Arrays.stream(Address.values()).map(Address::getShortName).toList()));

        // 비어 있으면 필드를 아예 넣지 않는다. 빈 enum 은 고를 수 있는 값이 없는데 필드만 있는 꼴이 된다
        if (!vocab.types().isEmpty()) {
            props.put("type", Map.of(
                    "type", "string",
                    "description", "업종. 애매하면 생략한다",
                    "enum", vocab.types()));
        }
        if (!vocab.categories().isEmpty()) {
            props.put("categories", Map.of(
                    "type", "array",
                    "description", "분위기·특징 태그. 사용자 말과 가까운 것 2~4개. 목록에 있는 것만 쓴다",
                    "items", Map.of("type", "string", "enum", vocab.categories())));
        }
        props.put("keyword", Map.of(
                "type", "string",
                "description", "고유명사나 상세주소 (예: 감천문화마을, 해운대구). 없으면 생략"));

        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name(),
                        "description", "등록된 여행지를 찾는다. 지역은 필수다",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", props,
                                "required", List.of("area"))));
    }

    @Override
    public String execute(JsonNode args) {
        Address area = parseArea(args.path("area").asString(null));
        String city = blankToNull(args.path("city").asString(null));
        if (area == null && city == null) {
            return "지역을 알 수 없어 검색하지 못했습니다. 사용자에게 어느 지역인지 물어보세요.";
        }

        String type = args.path("type").asString(null);
        List<String> categories = readStrings(args.path("categories"));
        String keyword = args.path("keyword").asString(null);

        List<PlaceHitDTO> hits = placeSearchService.searchForAi(
                area, city, type, categories, keyword, FETCH_LIMIT, SEND_LIMIT);

        // 엉뚱한 지역·업종이 나왔을 때 모델이 뭘 보냈는지 봐야 원인을 안다
        log.info("search_places area={} city={} type={} categories={} keyword={} -> {}곳",
                area == null ? null : area.getShortName(), city, type, categories, keyword, hits.size());

        if (hits.isEmpty()) {
            // 다른 지역으로 바꿔 찾지 않는다. 없으면 없다고 말한다
            return city == null ? NOT_FOUND : city + NOT_FOUND_IN_CITY;
        }
        return jsonMapper.writeValueAsString(hits.stream().map(this::toMap).toList());
    }

    @Override
    public String directReply(String result) {
        if (NOT_FOUND.equals(result)) {
            return NOT_FOUND_REPLY;
        }
        if (result.endsWith(NOT_FOUND_IN_CITY)) {
            String city = result.substring(0, result.length() - NOT_FOUND_IN_CITY.length());
            return "아쉽지만 " + city + " 쪽에는 아직 등록된 장소가 없어요 😢\n"
                    + "가까운 다른 지역을 말씀해 주시면 거기서 찾아볼게요!";
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // AI 가 잘못된 지역 코드를 보낼 수 있으므로 valueOf 를 감싼다
    private Address parseArea(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Address.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Address.of(value);   // "부산" 처럼 한글로 보냈을 때 구제
        }
    }

    private List<String> readStrings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode e : node) {
                String s = e.asString(null);
                if (s != null && !s.isBlank()) values.add(s.trim());
            }
        }
        return values;
    }

    // AI 에게 줄 필드만 추린다. 좌표는 프론트가 쓰는 값이라 칩 연결용으로만 보낸다
    private Map<String, Object> toMap(PlaceHitDTO p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("locationId", p.getLocationId());
        m.put("name", p.getLocationName());
        m.put("type", p.getTypeName());
        m.put("address", p.getLocationAddress());
        m.put("categories", p.getAllCategories());
        m.put("description", p.getLocationDescription());
        m.put("rating", p.getLocationReviewAvg());
        // matched 가 비어있으면 조건에 맞은 게 아니다. AI 가 이걸 보고 솔직하게 말한다
        m.put("matched", p.getMatchedCategories());
        return m;
    }
}
