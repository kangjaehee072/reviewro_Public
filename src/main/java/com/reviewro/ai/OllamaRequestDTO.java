package com.reviewro.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaRequestDTO {

    private String model;
    private List<AiMessageDTO> messages;
    private boolean stream;
    private boolean think;        // 추론 과정 끄기 (안 끄면 content 가 빈 채로 온다)

    // AI 가 쓸 수 있는 도구 목록. null 이면 필드가 빠지고, 그러면 모델이 도구를 못 부른다.
    // 2라운드(검색 결과를 넣고 답을 만드는 단계)에서 일부러 null 을 넘겨 재검색을 막는다
    private List<Map<String, Object>> tools;

    private Options options;

    @Getter
    @AllArgsConstructor
    public static class Options {
        @JsonProperty("num_ctx")
        private int numCtx;
        @JsonProperty("num_predict")
        private int numPredict;
    }
}
