package com.reviewro.ai;

import tools.jackson.databind.JsonNode;

import java.util.Map;

// AI 가 호출할 수 있는 도구. AiService 가 이 인터페이스만 알고,
// 실제 구현(DB 검색 등)은 각 도메인 패키지에 둔다.
// 새 도구를 추가할 때는 구현체를 빈으로 만들기만 하면 AiService 는 안 건드린다.
public interface AiTool {

    // Ollama 에 보낼 함수 이름. AI 가 이 이름으로 호출해 온다
    String name();

    // tools 배열에 들어갈 JSON 스키마. 호출할 때마다 만들어서 선택지를 DB 최신값으로 유지한다
    Map<String, Object> schema();

    // AI 가 넘긴 인자로 실제 작업을 하고, 결과를 AI 에게 돌려줄 문자열로 만든다
    String execute(JsonNode arguments);

    // 이 결과면 모델에게 다시 묻지 않고 사용자에게 바로 보낼 문장. null 이면 평소대로 모델이 답을 만든다.
    // 빈 결과를 받은 모델은 아는 장소로 빈칸을 채워 넣기 때문에, 그런 경우를 여기서 끊는다
    default String directReply(String result) {
        return null;
    }
}
