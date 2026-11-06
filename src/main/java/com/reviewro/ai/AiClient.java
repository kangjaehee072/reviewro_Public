package com.reviewro.ai;

import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.function.Consumer;

// 모델 서버 하나(Ollama, Claude)를 감싼다.
// AiService·AiSearchExtractor 는 이것만 알고, 어느 쪽을 쓸지는 AiSettingService 가 고른다
public interface AiClient {

    // 대화 답을 글자 조각으로 흘려보낸다. 다 끝나면 돌아온다.
    // messages 는 user / assistant / tool 역할이 섞인 대화 그대로다 (system 은 따로 받는다)
    void stream(String system, List<AiMessageDTO> messages, Consumer<String> onToken);

    // 스키마에 맞는 JSON 한 덩어리를 받아 문자열로 돌려준다 (검색 조건 뽑기용, 짧은 답)
    default String json(String system, String user, ObjectNode schema) {
        return json(system, user, schema, 200);
    }

    // 긴 JSON 이 필요할 때 (여행 가이드 등). maxTokens 는 답의 최대 길이
    String json(String system, String user, ObjectNode schema, int maxTokens);
}
