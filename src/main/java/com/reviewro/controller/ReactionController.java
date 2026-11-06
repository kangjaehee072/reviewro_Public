package com.reviewro.controller;

import com.reviewro.dto.ReactionDTO;
import com.reviewro.entity.ReactionKind;
import com.reviewro.entity.TargetType;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    @GetMapping
    public ReactionDTO read(@RequestParam TargetType type,
                            @RequestParam Long id,
                            @AuthenticationPrincipal MemberDetails details) {
        return reactionService.read(type, id,
                details == null ? null : details.getMember().getMemberId());
    }

    @PostMapping
    public ReactionDTO toggle(@RequestBody Map<String, String> body,
                              @AuthenticationPrincipal MemberDetails details) {
        if (details == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return reactionService.toggle(
                    TargetType.valueOf(body.get("type")),
                    Long.valueOf(body.get("id")),
                    details.getMember().getMemberId(),
                    ReactionKind.valueOf(body.get("kind")));

        } catch (DataIntegrityViolationException e) {
            // 아주 빠르게 두 번 눌러서 유니크 제약에 걸린 경우
            throw new ResponseStatusException(HttpStatus.CONFLICT, "잠시 후 다시 시도해 주세요.");
        }
    }
}