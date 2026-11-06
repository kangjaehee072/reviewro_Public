package com.reviewro.controller;

import com.reviewro.dto.PhotoMapDTO;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.PhotoMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/contents/photomap")
@RequiredArgsConstructor
@Log4j2
public class PhotoMapController {

    private final PhotoMapService photoMapService;

    @GetMapping
    public String list(@AuthenticationPrincipal MemberDetails details, Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        model.addAttribute("photoMaps", photoMapService.findMine(details.getMember().getMemberId()));
        return "contents/photomap/list";
    }

    @GetMapping("/new")
    public String createForm(@AuthenticationPrincipal MemberDetails details, Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        model.addAttribute("photoMap", new PhotoMapDTO());
        return "contents/photomap/form";
    }

    @GetMapping("/{photoMapId}")
    public String editForm(@PathVariable Long photoMapId, @AuthenticationPrincipal MemberDetails details, Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        model.addAttribute("photoMap", photoMapService.findOne(photoMapId, details.getMember().getMemberId()));
        return "contents/photomap/form";
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<Long> create(@ModelAttribute PhotoMapDTO dto, @AuthenticationPrincipal MemberDetails details) {
        if (details == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(photoMapService.create(details.getMember().getMemberId(), dto));
    }

    @PatchMapping("/{photoMapId}")
    @ResponseBody
    public ResponseEntity<Long> update(@PathVariable Long photoMapId, @ModelAttribute PhotoMapDTO dto,
                                       @AuthenticationPrincipal MemberDetails details) {
        if (details == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        photoMapService.update(photoMapId, details.getMember().getMemberId(), dto);
        return ResponseEntity.ok(photoMapId);
    }

    @DeleteMapping("/{photoMapId}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long photoMapId, @AuthenticationPrincipal MemberDetails details) {
        if (details == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        photoMapService.delete(photoMapId, details.getMember().getMemberId());
        return ResponseEntity.ok().build();
    }

    // 우리가 사용자에게 보여 주려고 만든 오류(IllegalArgumentException)만 문구를 그대로 돌려준다
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // 그 밖의 오류(DB·파일 등)는 메시지에 SQL·컬럼명 같은 내부 정보가 섞일 수 있어 로그에만 남긴다
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResponseEntity<String> handleUnexpected(RuntimeException e) {
        log.error("포토맵 처리 중 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리하지 못했어요. 잠시 후 다시 시도해 주세요.");
    }
}
