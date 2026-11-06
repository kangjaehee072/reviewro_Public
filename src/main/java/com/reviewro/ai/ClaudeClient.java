package com.reviewro.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// Anthropic Claude API. 모델은 관리자 설정을 따르므로 withModel 로 골라서 쓴다
@Log4j2
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AiProperties.class)
public class ClaudeClient {

    private static final long CHAT_MAX_TOKENS = 16000L;
    private static final long JSON_MAX_TOKENS = 4000L;

    // 거절(refusal)되면 서버가 알아서 다른 모델로 다시 답하게 한다. Haiku 는 지원하지 않는다
    private static final List<String> FALLBACK_MODELS = List.of("claude-opus-5-5", "claude-sonnet-5-5");

    private final AiProperties aiProperties;
    private final JsonMapper jsonMapper;

    private volatile AnthropicClient client;

    public AiClient withModel(String model) {
        return new Bound(model);
    }

    // 키는 .env 에서 읽어 직접 넘긴다 (.env 값은 진짜 환경변수가 아니라 fromEnv 로는 못 읽는다)
    private AnthropicClient client() {
        if (!aiProperties.hasClaudeKey()) {
            throw new AiException("Claude API 키가 설정되지 않았어요.");
        }
        AnthropicClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    client = AnthropicOkHttpClient.builder()
                            .apiKey(aiProperties.claude().apiKey())
                            .build();
                }
                c = client;
            }
        }
        return c;
    }

    private class Bound implements AiClient {

        private final String model;

        Bound(String model) {
            this.model = model;
        }

        @Override
        public void stream(String system, List<AiMessageDTO> history, Consumer<String> onToken) {
            MessageCreateParams.Builder params = base(system, CHAT_MAX_TOKENS)
                    .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                    // 도구를 안 주는데도 도구 호출을 글로 흉내 낼 때가 있다. 그 앞에서 끊어 화면에 안 나가게 한다
                    .stopSequences(List.of("<invoke", "<function_calls", "<parameter"));
            addMessages(params, history);

            AtomicBoolean wrote = new AtomicBoolean(false);
            AtomicReference<StopReason> stop = new AtomicReference<>();
            call(() -> {
                try (StreamResponse<RawMessageStreamEvent> response = client().messages().createStreaming(params.build())) {
                    response.stream().forEach(event -> {
                        event.contentBlockDelta()
                                .flatMap(delta -> delta.delta().text())
                                .ifPresent(text -> {
                                    wrote.set(true);
                                    onToken.accept(text.text());
                                });
                        event.messageDelta()
                                .flatMap(delta -> delta.delta().stopReason())
                                .ifPresent(stop::set);
                    });
                }
                return null;
            });

            if (StopReason.REFUSAL.equals(stop.get()) && !wrote.get()) {
                throw new AiException("이 질문에는 답할 수 없어요. 여행 이야기로 다시 물어봐 주세요.");
            }
        }

        @Override
        public String json(String system, String user, ObjectNode schema, int maxTokens) {
            // 생각(thinking)도 이 한도 안에서 쓰므로 짧은 답이어도 바닥을 깔아 둔다
            MessageCreateParams params = base(system, Math.max(JSON_MAX_TOKENS, maxTokens))
                    .outputConfig(OutputConfig.builder()
                            .effort(OutputConfig.Effort.LOW)
                            .format(JsonOutputFormat.builder()
                                    .schema(JsonOutputFormat.Schema.builder()
                                            .putAllAdditionalProperties(toJsonValues(claudeSchema(schema)))
                                            .build())
                                    .build())
                            .build())
                    .addUserMessage(user)
                    .build();

            Message message = call(() -> client().messages().create(params));
            if (message.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                return "";
            }
            return message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .collect(Collectors.joining());
        }

        // 시스템 프롬프트는 매번 같으므로 캐시해서 입력 비용을 줄인다
        private MessageCreateParams.Builder base(String system, long maxTokens) {
            MessageCreateParams.Builder params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(maxTokens)
                    .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                            .text(system)
                            .cacheControl(CacheControlEphemeral.builder().build())
                            .build()));
            if (FALLBACK_MODELS.contains(model)) {
                params.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01");
                params.putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
            }
            return params;
        }
    }

    // Ollama 형식 대화를 Claude 형식으로 바꾼다.
    // Claude 는 user/assistant 가 번갈아 와야 하고 첫 말은 user 여야 한다.
    // 검색 호출(assistant + tool_calls)은 건너뛰고, 검색 결과(tool)는 사용자 쪽 자료로 넣는다
    private void addMessages(MessageCreateParams.Builder params, List<AiMessageDTO> history) {
        List<String[]> turns = new ArrayList<>();   // {role, text}
        for (AiMessageDTO m : history) {
            String content = m.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            String role;
            String text;
            if ("tool".equals(m.getRole())) {
                role = "user";
                text = "[장소 검색 결과]\n" + content;
            } else if ("assistant".equals(m.getRole())) {
                role = "assistant";
                text = content;
            } else {
                role = "user";
                text = content;
            }

            if (turns.isEmpty() && role.equals("assistant")) {
                continue;   // 인사말처럼 AI 가 먼저 한 말은 뺀다
            }
            if (!turns.isEmpty() && turns.get(turns.size() - 1)[0].equals(role)) {
                String[] last = turns.get(turns.size() - 1);
                last[1] = last[1] + "\n\n" + text;
            } else {
                turns.add(new String[]{role, text});
            }
        }

        for (String[] turn : turns) {
            if (turn[0].equals("user")) {
                params.addUserMessage(turn[1]);
            } else {
                params.addAssistantMessage(turn[1]);
            }
        }
    }

    // Ollama 용 스키마를 Claude 구조화 출력이 받는 모양으로 바꾼다.
    //   type: ["string","null"]  →  anyOf 로 나눈다 (enum 은 string 쪽으로)
    //   object 에는 additionalProperties: false 가 꼭 있어야 한다
    private ObjectNode claudeSchema(JsonNode node) {
        ObjectNode out = jsonMapper.createObjectNode();
        JsonNode type = node.get("type");

        if (type != null && type.isArray()) {
            ArrayNode anyOf = out.putArray("anyOf");
            for (JsonNode t : type) {
                ObjectNode branch = jsonMapper.createObjectNode();
                branch.put("type", t.asString());
                if (!"null".equals(t.asString()) && node.has("enum")) {
                    ArrayNode values = branch.putArray("enum");
                    for (JsonNode v : node.get("enum")) {
                        if (!v.isNull()) {
                            values.add(v);
                        }
                    }
                }
                anyOf.add(branch);
            }
            return out;
        }

        node.properties().forEach(e -> {
            switch (e.getKey()) {
                case "properties" -> {
                    ObjectNode props = out.putObject("properties");
                    e.getValue().properties().forEach(p -> props.set(p.getKey(), claudeSchema(p.getValue())));
                }
                case "items" -> out.set("items", claudeSchema(e.getValue()));
                default -> out.set(e.getKey(), e.getValue());
            }
        });
        if ("object".equals(node.path("type").asString(""))) {
            out.put("additionalProperties", false);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, JsonValue> toJsonValues(ObjectNode schema) {
        Map<String, Object> map = jsonMapper.convertValue(schema, Map.class);
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> JsonValue.from(e.getValue())));
    }

    // SDK 예외를 화면용 문장으로 바꾼다. 원문(키·모델 사정)은 로그에만 남긴다
    private <T> T call(SdkCall<T> body) {
        try {
            return body.run();
        } catch (RateLimitException e) {
            log.warn("Claude 요청 한도 초과: {}", e.getMessage());
            throw new AiException("지금 질문이 몰려서 답을 못 만들었어요. 잠시 후 다시 시도해 주세요.", e);
        } catch (UnauthorizedException | PermissionDeniedException e) {
            // 키 자체는 남기지 않고 모양만 남긴다 (일반 API 키는 sk-ant-api 로 시작한다)
            String key = aiProperties.claude().apiKey();
            log.error("Claude API 키 오류 (키 길이 {}, sk-ant-api 로 시작 {}): {}",
                    key.length(), key.startsWith("sk-ant-api"), e.getMessage());
            throw new AiException("AI 설정에 문제가 있어요. 관리자에게 알려 주세요.", e);
        } catch (AnthropicServiceException e) {
            log.warn("Claude 서버 오류 {}: {}", e.statusCode(), e.getMessage());
            throw new AiException("AI 가 답을 만들지 못했어요. 잠시 후 다시 시도해 주세요.", e);
        } catch (AnthropicIoException e) {
            log.warn("Claude 연결 실패: {}", e.getMessage());
            throw new AiException("AI 서버에 연결할 수 없습니다.", e);
        }
    }

    @FunctionalInterface
    private interface SdkCall<T> {
        T run();
    }
}
