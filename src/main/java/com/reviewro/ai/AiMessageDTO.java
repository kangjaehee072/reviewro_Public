package com.reviewro.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)   // null 필드를 보내면 Ollama 가 싫어한다
public class AiMessageDTO {

    private String role;      // user / assistant / tool
    private String content;

    // 모델이 도구를 부를 때 담겨 오는 값. 받은 구조를 그대로 다음 요청에 되돌려주면 되므로
    // 타입을 만들지 않고 JsonNode 로 들고 간다
    @JsonProperty("tool_calls")
    private List<JsonNode> toolCalls;

    // role=tool 일 때 어떤 도구의 결과인지
    @JsonProperty("tool_name")
    private String toolName;

    public AiMessageDTO(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
