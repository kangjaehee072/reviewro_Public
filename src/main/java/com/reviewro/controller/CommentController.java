package com.reviewro.controller;

import com.reviewro.dto.CommentDTO;
import com.reviewro.entity.TargetType;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    // 목록은 로그인 없이도 볼 수 있다
    @GetMapping
    public List<CommentDTO> list(@RequestParam TargetType type,
                                 @RequestParam Long id,
                                 @AuthenticationPrincipal MemberDetails details) {
        return commentService.list(type, id, memberId(details));
    }

    @PostMapping
    public List<CommentDTO> write(@RequestBody Map<String, Object> body,
                                  @AuthenticationPrincipal MemberDetails details) {
        requireLogin(details);

        TargetType type = TargetType.valueOf((String) body.get("type"));
        Long id = Long.valueOf(body.get("id").toString());
        Long parentId = body.get("parentId") == null
                ? null : Long.valueOf(body.get("parentId").toString());

        commentService.write(type, id, details.getMember().getMemberId(),
                (String) body.get("content"), parentId);

        // 쓴 뒤 목록을 그대로 돌려준다 -> 프론트가 다시 안 물어봐도 된다
        return commentService.list(type, id, details.getMember().getMemberId());
    }

    @DeleteMapping("/{commentId}")
    public void delete(@PathVariable Long commentId,
                       @AuthenticationPrincipal MemberDetails details) {
        requireLogin(details);
        commentService.delete(commentId, details.getMember().getMemberId());
    }

    private Long memberId(MemberDetails details) {
        return details == null ? null : details.getMember().getMemberId();
    }

    private void requireLogin(MemberDetails details) {
        if (details == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

    }
}
