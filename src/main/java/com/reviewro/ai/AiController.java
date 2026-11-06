package com.reviewro.ai;

import com.reviewro.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private static final int MAX_MESSAGES = 30;
    private static final int MAX_CONTENT_LENGTH = 10000;
    // tool 은 서버가 도구 결과를 대화에 붙일 때 쓰는 역할.
    // 프론트가 다음 턴에 그대로 되돌려주므로 받아줘야 한다
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant", "tool");

    // 회원당 10분에 60회. 도구를 쓰면 한 번의 질문에 모델을 2번 부르므로 넉넉히 둔다
    private static final int RATE_LIMIT = 60;
    private static final long RATE_WINDOW_MS = 10 * 60 * 1000L;
    private final Map<Long, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    private final AiService aiService;

    // 페르소나 목록 (나중에 챗창에서 선택용)
    @GetMapping("/personas")
    public List<AiPersonaDTO> personas() {
        return Arrays.stream(AiPersona.values())
                .map(p -> new AiPersonaDTO(p.name(), p.getDisplayName()))
                .toList();
    }

    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody AiChatRequestDTO request,
                           @AuthenticationPrincipal MemberDetails details) {
        if (details == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        checkRateLimit(details.getMember().getMemberId());
        validate(request);
        return aiService.stream(request.getPersona(), request.getMessages());
    }

    private void validate(AiChatRequestDTO request) {
        if (request.getPersona() == null || request.getMessages() == null
                || request.getMessages().isEmpty() || request.getMessages().size() > MAX_MESSAGES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
        }
        for (AiMessageDTO m : request.getMessages()) {
            if (!ALLOWED_ROLES.contains(m.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
            }
            // 도구 호출 메시지는 content 가 비어 있고 tool_calls 에 내용이 들어있다.
            // 그래도 content 를 같이 실어 길이 제한을 피하지 못하게 길이는 항상 본다
            boolean isToolCall = m.getToolCalls() != null && !m.getToolCalls().isEmpty();
            if (isToolCall) {
                if ((m.getContent() != null && m.getContent().length() > MAX_CONTENT_LENGTH)
                        || m.getToolCalls().toString().length() > MAX_CONTENT_LENGTH) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
                }
                continue;
            }
            if (m.getContent() == null || m.getContent().isBlank()
                    || m.getContent().length() > MAX_CONTENT_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
            }
        }
    }

    private void checkRateLimit(Long memberId) {
        long now = System.currentTimeMillis();
        Deque<Long> hits = requestLog.computeIfAbsent(memberId, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > RATE_WINDOW_MS) hits.pollFirst();
            if (hits.size() >= RATE_LIMIT) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 잦습니다.");
            }
            hits.addLast(now);
        }
    }
}