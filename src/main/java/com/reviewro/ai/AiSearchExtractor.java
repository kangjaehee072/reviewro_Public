package com.reviewro.ai;

import com.reviewro.entity.Address;
import com.reviewro.service.AiVocab;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 대화에서 검색 조건만 뽑는 전담 호출.
 *
 * 왜 따로 부르는가:
 * 한 번에 도구 호출까지 시키면 모델이 "말투 지키기 + 도구 부를지 정하기 + 인자 채우기" 를
 * 동시에 해야 해서 자꾸 틀렸다. 부산이라 해놓고 area=대구 를 보내거나,
 * 카페를 찾는데 type 을 아예 안 보내는 식이었다.
 *
 * 여기서는 말투도 이모지도 없이 JSON 만 뽑게 한다.
 * 게다가 format 에 스키마를 넣어 값 자체를 목록 안에서만 고르게 묶는다.
 * (측정: 앞턴에 부산을 말하고 이번 턴에 "카페" 라고 한 경우 5/5 로 부산+카페 를 뽑았다.
 *  같은 상황에서 도구 호출은 대구를 보냈다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiSearchExtractor {

    // 대화를 통째로 넣으면 느리고, 오래된 맥락이 지역 판단을 흐린다
    private static final int MAX_MESSAGES = 10;

    private static final String SYSTEM = """
            너는 사용자 말에서 장소 검색 조건만 뽑는 도구다. 설명하지 말고 JSON 하나만 낸다.
            입력은 [이전 사용자 말], [직전 AI 말](있을 때만), [마지막 사용자 말] 로 나뉘어 온다.
            지역(city, area)은 사용자 말을 보고 사용자가 가장 최근에 말한 곳을 쓴다. 말한 적 없으면 둘 다 null. 지어내지 않는다.
            단, [직전 AI 말] 이 지역을 제안했고(예: "대구 어때요?") [마지막 사용자 말] 이 그 제안에 동의하는 말이면
            (예: "그래", "좋아 찾아봐") 그 지역을 쓴다. 거절하거나 다른 곳을 말했으면 그 지역을 쓰지 않는다.
            [직전 AI 말] 에서는 지역만 본다. type, categories, keyword 는 거기서 뽑지 않는다.
            type, categories, keyword 는 [마지막 사용자 말] 에서만 뽑는다. 이전 말의 업종을 이어받지 않는다.
            마지막 말에 업종이 분명히 없으면(예: "분위기 좋은 곳", "갈만한 데") type 은 null 이다.
            city 는 사용자가 말한 시·군·구·동네 이름을 그대로 쓴다 (예: 서산, 해운대, 강남, 전주).
            시·도 이름만 말했으면(예: 부산, 충남, 제주도) city 는 null 이다.
            area 는 그 지역이 속한 시/도다. city 가 있으면 네가 아는 지리로 알맞은 시/도를 고른다
            (예: 서산 → 충남, 해운대 → 부산, 전주 → 전북, 강릉 → 강원). 비슷한 글자의 다른 시/도를 고르지 않는다.
            type 은 사용자가 찾는 업종이다. 분명하지 않으면 null.
            categories 는 분위기나 특징이다. 없으면 빈 배열.
            keyword 는 고유명사나 상세주소가 있을 때만. 없으면 null.
            anywhere 는 사용자가 지역을 안 가리겠다고 직접 말했을 때만 true 다.
            (아무 데나 / 너 맘대로 / 알아서 / 상관없어 / 전국)
            그냥 지역을 말하지 않은 것뿐이면 false 다. 그때는 어느 지역인지 물어봐야 하기 때문이다.
            """;

    private final JsonMapper jsonMapper;
    private final AiSettingService aiSettingService;
    private final AiVocab aiVocab;

    /** 뽑아낸 조건. 못 뽑으면 전부 비어 있는 채로 온다 (→ 지역을 되묻게 된다) */
    // area: 시/도 (Address 의 짧은 이름), city: 사용자가 말한 시·군·구 이름 그대로
    public record Extracted(String area, String city, String type,
                            List<String> categories, String keyword, boolean anywhere) {

        public static Extracted empty() {
            return new Extracted(null, null, null, List.of(), null, false);
        }
    }

    public Extracted extract(List<AiMessageDTO> history) {
        try {
            String content = aiSettingService.client().json(SYSTEM, input(history), schema());

            Extracted raw = parse(content);
            // 직전 AI 말도 근거로 친다. AI 가 "대구 어때요?" 하고 사용자가 동의했으면 대구는 사용자가 고른 것이다
            // (동의인지는 모델이 판단한다. 동의가 아니면 모델이 그 지역을 고르지 않는다)
            Extracted want = grounded(raw, userText(history) + lastAssistantText(history), lastUserText(history));
            log.info("추출 area={} city={} type={} categories={} keyword={} anywhere={} (모델 원본 area={} city={})",
                    want.area(), want.city(), want.type(), want.categories(), want.keyword(), want.anywhere(),
                    raw.area(), raw.city());
            return want;

        } catch (Exception e) {
            // 못 뽑으면 지역을 되묻는 쪽으로 간다. 지어낸 값으로 검색하는 것보다 낫다
            log.warn("검색 조건을 뽑지 못했습니다: {}", e.toString());
            return Extracted.empty();
        }
    }

    private Extracted parse(String content) {
        if (content == null || content.isBlank()) {
            return Extracted.empty();
        }
        JsonNode node = jsonMapper.readTree(content);

        List<String> categories = new ArrayList<>();
        for (JsonNode c : node.path("categories")) {
            String value = c.asString(null);
            if (value != null && !value.isBlank()) {
                categories.add(value.trim());
            }
        }

        return new Extracted(
                text(node.path("area")),
                text(node.path("city")),
                text(node.path("type")),
                categories,
                text(node.path("keyword")),
                node.path("anywhere").asBoolean(false));
    }

    // ===== 지역 검증 =====
    // 지역 판단(서산 → 충남)은 모델에게 맡기되, 사용자가 말한 적 없는 지역은 쓰지 않는다.
    // 예전에 고를 수 있는 값이 시/도뿐이라 "서산" 을 글자가 비슷한 "서울" 로 바꿔 검색한 일이 있었다.
    // 그런 값이 오면 버리고 되묻는 쪽으로 보낸다 (엉뚱한 지역을 추천하는 것보다 낫다)
    private Extracted grounded(Extracted want, String said, String last) {
        String city = want.city();
        if (city != null && !said.contains(stem(city))) {
            city = null;
        }
        String area = want.area();
        if (area != null && city == null && !mentionsArea(area, said)) {
            area = null;
        }
        // 업종도 마지막 말에 그 업종을 뜻하는 말이 있을 때만 쓴다.
        // 예전에 "숙소 추천" 뒤로 "분위기 좋은 곳" 까지 숙박으로 이어받아 숙소만 추천하던 일이 있었다
        String type = want.type();
        if (type != null && !mentions(TYPE_WORDS, type, last)) {
            type = null;
        }
        // 태그는 검색 순서를 정하는 가점이라, 엉뚱한 태그(분위기 좋은 곳 → 기념품)가 붙으면 그 업종이 위로 올라온다
        List<String> categories = want.categories().stream()
                .filter(c -> mentions(CATEGORY_WORDS, c, last))
                .toList();
        return new Extracted(area, city, type, categories, want.keyword(), want.anywhere());
    }

    // 마지막 사용자 말 바로 앞의 AI 답. 지역 제안만 보면 되니 앞부분만 쓴다
    // (추천 목록 전체를 넣으면 길고, 목록 속 업종·태그가 조건에 섞여 들어온다)
    private static final int MAX_PROPOSAL_CHARS = 300;

    private String lastAssistantText(List<AiMessageDTO> history) {
        int i = history.size() - 1;
        while (i >= 0 && !"user".equals(history.get(i).getRole())) {
            i--;
        }
        for (i--; i >= 0; i--) {
            AiMessageDTO m = history.get(i);
            if ("user".equals(m.getRole())) {
                return "";
            }
            if ("assistant".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                String text = m.getContent().trim();
                return text.length() > MAX_PROPOSAL_CHARS ? text.substring(0, MAX_PROPOSAL_CHARS) : text;
            }
        }
        return "";
    }

    // 업종을 가리키는 흔한 말. 표에 없는 업종(관리자가 새로 만든 것)은 업종 이름 자체로 본다
    private static final Map<String, List<String>> TYPE_WORDS = Map.of(
            "음식점", List.of("음식", "맛집", "식당", "밥", "먹", "요리", "고기", "횟집", "국밥", "점심", "저녁", "아침"),
            "카페/디저트", List.of("카페", "커피", "디저트", "빵", "베이커리", "케이크"),
            "자연/관광명소", List.of("관광", "명소", "자연", "공원", "등산", "산책", "바다", "해변", "계곡", "숲", "풍경", "경치"),
            "숙박", List.of("숙박", "숙소", "호텔", "모텔", "펜션", "게스트하우스", "리조트", "잘 곳", "잘곳", "묵을", "자고"),
            "문화/예술", List.of("문화", "예술", "전시", "박물관", "미술관", "공연", "갤러리"),
            "액티비티/체험", List.of("액티비티", "체험", "레저", "놀거리", "놀 거리", "활동"),
            "축제/행사", List.of("축제", "행사", "페스티벌"),
            "쇼핑", List.of("쇼핑", "시장", "쇼핑몰", "아울렛", "기념품"));

    // 분위기·특징 태그를 가리키는 흔한 말. 표에 없는 태그는 태그 이름 자체로 본다
    private static final Map<String, List<String>> CATEGORY_WORDS = Map.ofEntries(
            Map.entry("맛집", List.of("맛집", "맛있")),
            Map.entry("카페", List.of("카페", "커피")),
            Map.entry("디저트", List.of("디저트", "빵", "케이크", "달달")),
            Map.entry("역사", List.of("역사", "유적", "고궁", "전통", "문화재")),
            Map.entry("오션뷰", List.of("오션", "바다", "해변", "바닷가")),
            Map.entry("애견", List.of("애견", "강아지", "반려", "댕댕")),
            Map.entry("가족", List.of("가족", "아이", "아기", "애들", "부모님", "어린이")),
            Map.entry("체험", List.of("체험", "만들기", "놀거리", "액티비티")),
            Map.entry("기념품", List.of("기념품", "선물", "특산")),
            Map.entry("포토존", List.of("포토", "사진", "인생샷", "인스타")),
            Map.entry("데이트", List.of("데이트", "연인", "커플", "분위기", "로맨틱", "여친", "남친", "애인")),
            Map.entry("야경명소", List.of("야경", "노을", "밤에", "밤 ")),
            Map.entry("식도락", List.of("식도락", "먹방", "먹거리", "먹으러")),
            Map.entry("가성비", List.of("가성비", "저렴", "싼 ", "싸게", "가격")),
            Map.entry("로컬맛집", List.of("로컬", "현지", "토박이", "숨은")));

    // 사용자 마지막 말에 그 업종·태그를 가리키는 말이 있는지
    private boolean mentions(Map<String, List<String>> table, String name, String last) {
        List<String> words = table.get(name);
        if (words == null) {
            words = List.of(name.split("/"));
        }
        return words.stream().anyMatch(last::contains);
    }

    private String lastUserText(List<AiMessageDTO> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            AiMessageDTO m = history.get(i);
            if ("user".equals(m.getRole()) && m.getContent() != null) {
                return m.getContent();
            }
        }
        return "";
    }

    // "서산시" → "서산", "해운대구" → "해운대". 사용자는 보통 끝 글자를 빼고 말한다
    public static String stem(String place) {
        String p = place.trim();
        if (p.length() > 2 && p.matches(".*[시군구읍면동]$")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private boolean mentionsArea(String area, String said) {
        Address a = Address.of(area);
        return a != null && (said.contains(a.getShortName()) || said.contains(a.getFullName()));
    }

    // 최근 사용자 말만 이어 붙인다 (AI 답이나 검색 결과에 나온 지역은 사용자가 말한 게 아니다)
    private String userText(List<AiMessageDTO> history) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = history.size() - 1; i >= 0 && count < MAX_MESSAGES; i--) {
            AiMessageDTO m = history.get(i);
            if ("user".equals(m.getRole()) && m.getContent() != null) {
                sb.append(m.getContent()).append('\n');
                count++;
            }
        }
        return sb.toString();
    }

    private String text(JsonNode node) {
        String value = node.asString(null);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // ===== 요청 만들기 =====

    // 사용자 말만 넣는다. AI 답(추천 목록·#숙박 같은 태그)을 넣으면
    // 한 번 숙소를 추천한 뒤로 다음 질문에서도 업종을 숙박으로 뽑아서 점점 숙소만 추천하게 된다.
    // 지역은 이전 말에서 이어받아야 하므로 이전 말도 넣되, 마지막 말과 나눠서 보여준다
    private String input(List<AiMessageDTO> history) {
        List<String> said = new ArrayList<>();
        for (AiMessageDTO m : history) {
            if ("user".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                said.add(m.getContent().trim());
            }
        }
        if (said.size() > MAX_MESSAGES) {
            said = said.subList(said.size() - MAX_MESSAGES, said.size());
        }

        StringBuilder input = new StringBuilder("[이전 사용자 말]\n");
        if (said.size() <= 1) {
            input.append("(없음)\n");
        } else {
            for (String s : said.subList(0, said.size() - 1)) {
                input.append("- ").append(s).append('\n');
            }
        }
        String proposal = lastAssistantText(history);
        if (!proposal.isEmpty()) {
            input.append("\n[직전 AI 말]\n").append(proposal).append('\n');
        }
        input.append("\n[마지막 사용자 말]\n").append(said.isEmpty() ? "" : said.get(said.size() - 1));
        return input.toString();
    }

    // 고를 수 있는 값을 스키마에 박아 넣는다.
    // 관리자가 업종·태그를 추가하면 다음 호출부터 바로 반영된다
    private ObjectNode schema() {
        AiVocab.Vocab vocab = aiVocab.load();

        List<String> areas = new ArrayList<>();
        for (Address a : Address.values()) {
            areas.add(a.getShortName());
        }

        // city 를 area 앞에 둔다. 모델은 앞에서부터 채우므로 "서산" 을 먼저 적고 나서 시/도를 고르게 된다
        ObjectNode props = jsonMapper.createObjectNode();
        ObjectNode city = jsonMapper.createObjectNode();
        city.set("type", twoTypes("string", "null"));
        props.set("city", city);
        props.set("area", nullableEnum(areas));
        props.set("type", nullableEnum(vocab.types()));
        props.set("categories", stringArray(vocab.categories()));

        ObjectNode keyword = jsonMapper.createObjectNode();
        keyword.set("type", twoTypes("string", "null"));
        props.set("keyword", keyword);

        ObjectNode anywhere = jsonMapper.createObjectNode();
        anywhere.put("type", "boolean");
        props.set("anywhere", anywhere);

        ArrayNode required = jsonMapper.createArrayNode();
        required.add("city");
        required.add("area");
        required.add("type");
        required.add("categories");
        required.add("keyword");
        required.add("anywhere");

        ObjectNode schema = jsonMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", props);
        schema.set("required", required);
        return schema;
    }

    // 목록에 있는 값 아니면 null. 둘 중 하나만 나오게 묶는다
    private ObjectNode nullableEnum(List<String> values) {
        ObjectNode node = jsonMapper.createObjectNode();
        node.set("type", twoTypes("string", "null"));

        ArrayNode allowed = jsonMapper.createArrayNode();
        for (String v : values) {
            allowed.add(v);
        }
        allowed.addNull();
        node.set("enum", allowed);
        return node;
    }

    private ObjectNode stringArray(List<String> values) {
        ArrayNode allowed = jsonMapper.createArrayNode();
        for (String v : values) {
            allowed.add(v);
        }

        ObjectNode items = jsonMapper.createObjectNode();
        items.put("type", "string");
        items.set("enum", allowed);

        ObjectNode node = jsonMapper.createObjectNode();
        node.put("type", "array");
        node.set("items", items);
        return node;
    }

    private ArrayNode twoTypes(String a, String b) {
        ArrayNode types = jsonMapper.createArrayNode();
        types.add(a);
        types.add(b);
        return types;
    }
}
