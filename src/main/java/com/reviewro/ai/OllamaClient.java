package com.reviewro.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 자체 서버의 Ollama (/api/chat)
@Log4j2
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AiProperties.class)
public class OllamaClient implements AiClient {

    private final JsonMapper jsonMapper;
    private final AiProperties aiProperties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public void stream(String system, List<AiMessageDTO> history, Consumer<String> onToken) {
        List<AiMessageDTO> messages = new ArrayList<>();
        messages.add(new AiMessageDTO("system", system));
        messages.addAll(history);

        // 도구는 아예 넘기지 않는다.
        // 부를지 말지와 인자 채우기는 AiSearchExtractor 가 이미 끝냈고,
        // 여기서 또 부를 수 있게 두면 엉뚱한 지역으로 다시 검색해 버린다
        OllamaRequestDTO body = new OllamaRequestDTO(
                aiProperties.model(), messages, true, false, null,
                new OllamaRequestDTO.Options(aiProperties.numCtx(), aiProperties.numPredict()));

        ByteArrayOutputStream pending = new ByteArrayOutputStream();   // 아직 글자가 못 된 바이트
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.endpoint() + "/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            jsonMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode node = jsonMapper.readTree(line);

                    if (node.hasNonNull("error")) {
                        // 모델 서버 원문(모델명·내부 사정)은 화면에 보내지 않고 로그에만 남긴다
                        log.warn("모델 서버 오류: {}", node.get("error").asString());
                        throw new AiException("AI 가 답을 만들지 못했어요. 잠시 후 다시 시도해 주세요.");
                    }

                    // 흔치 않은 한글은 모델이 바이트 토큰(<0xEB> 같은 것)으로 쪼개 보낸다.
                    // 그대로 흘리면 "눈에 <0xEB><0x9D><0x95>니다" 가 되므로 모았다가 글자로 합친다
                    String token = node.path("message").path("content").asString("");
                    String out = "";
                    if (!token.isEmpty()) {
                        Matcher byteToken = BYTE_TOKEN.matcher(token);
                        if (byteToken.matches()) {
                            pending.write(Integer.parseInt(byteToken.group(1), 16));
                        } else {
                            out = takePending(pending) + token;
                        }
                    }

                    boolean done = node.path("done").asBoolean(false);
                    if (done) {
                        out += takePending(pending);   // 끝났는데 남아 있으면 털어낸다
                    }
                    if (!out.isEmpty()) {
                        onToken.accept(out);
                    }
                    if (done) {
                        return;
                    }
                }
            }

        } catch (IOException e) {
            throw new AiException("모델 서버에 연결할 수 없습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("응답을 만드는 중 문제가 생겼습니다.", e);
        }
    }

    @Override
    public String json(String system, String user, ObjectNode schema, int maxTokens) {
        ArrayNode messages = jsonMapper.createArrayNode();
        messages.add(message("system", system));
        messages.add(message("user", user));

        ObjectNode body = jsonMapper.createObjectNode();
        body.put("model", aiProperties.model());
        body.put("stream", false);
        body.put("think", false);
        body.set("messages", messages);
        body.set("format", schema);

        // 같은 대화에 늘 같은 조건이 나와야 한다. 여기는 창의성이 필요한 자리가 아니다
        ObjectNode options = jsonMapper.createObjectNode();
        options.put("num_ctx", aiProperties.numCtx());
        options.put("num_predict", maxTokens);
        options.put("temperature", 0);
        body.set("options", options);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.endpoint() + "/chat"))
                    .header("Content-Type", "application/json")
                    // 짧은 조건 뽑기는 1분, 가이드처럼 긴 답은 자체 서버라 느려서 넉넉히 기다린다
                    .timeout(Duration.ofSeconds(maxTokens > 1000 ? 300 : 60))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            jsonMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return jsonMapper.readTree(response.body()).path("message").path("content").asString("");

        } catch (IOException e) {
            throw new AiException("모델 서버에 연결할 수 없습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("응답을 만드는 중 문제가 생겼습니다.", e);
        }
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = jsonMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    // 모델이 글자 하나를 못 만들 때 "<0xEB>" 처럼 바이트 하나씩 보낸다 (토크나이저 바이트 폴백)
    private static final Pattern BYTE_TOKEN = Pattern.compile("^<0x([0-9A-Fa-f]{2})>$");

    // 모아둔 바이트를 글자로 바꿔 돌려주고 비운다. 덜 모였으면 빈 문자열
    private String takePending(ByteArrayOutputStream pending) {
        if (pending.size() == 0) {
            return "";
        }
        String text = pending.toString(StandardCharsets.UTF_8);
        pending.reset();
        return text;
    }
}
