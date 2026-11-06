package com.reviewro.controller;

import com.reviewro.ai.AiException;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.TravelGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

// 여행 계획 → AI 가이드. 화면은 지도 페이지(여행 계획 짜기)의 '가이드' 전환 화면이다
// (/api/travels 는 SecurityConfig 에서 로그인 필요)
@RestController
@RequiredArgsConstructor
public class TravelGuideController {

    private final TravelGuideService travelGuideService;
    private final JsonMapper jsonMapper;

    // 화면이 그릴 재료: 사실(일정·장소·휴무 계산) + 저장해 둔 가이드(없으면 null)
    @GetMapping("/api/travels/{planId}/guide")
    public ResponseEntity<String> get(@PathVariable Long planId, @AuthenticationPrincipal MemberDetails details) {
        try {
            ObjectNode out = jsonMapper.createObjectNode();
            out.set("facts", jsonMapper.valueToTree(travelGuideService.facts(planId, details.getMember().getMemberId())));
            String saved = travelGuideService.saved(planId);
            out.set("saved", saved == null ? null : jsonMapper.readTree(saved));
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonMapper.writeValueAsString(out));
        } catch (IllegalArgumentException e) {
            return text(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // body: {} 이면 새로 만들고, {"instruction": "...", "current": {...}} 이면 그 부분만 고친다. 저장은 하지 않는다
    @PostMapping("/api/travels/{planId}/guide/draft")
    public ResponseEntity<String> draft(@PathVariable Long planId, @RequestBody(required = false) JsonNode body,
                                        @AuthenticationPrincipal MemberDetails details) {
        String instruction = body == null ? null : body.path("instruction").asString(null);
        JsonNode current = body == null ? null : body.get("current");
        try {
            String guide = travelGuideService.draft(planId, details.getMember().getMemberId(), instruction, current);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(guide);
        } catch (IllegalArgumentException e) {
            return text(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            return text(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        } catch (AiException e) {
            return text(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    @PutMapping("/api/travels/{planId}/guide")
    public ResponseEntity<String> save(@PathVariable Long planId, @RequestBody JsonNode guide,
                                       @AuthenticationPrincipal MemberDetails details) {
        try {
            travelGuideService.save(planId, details.getMember().getMemberId(), guide);
            return text(HttpStatus.OK, "저장했어요.");
        } catch (IllegalArgumentException e) {
            return text(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private ResponseEntity<String> text(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(message);
    }
}
