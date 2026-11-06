package com.reviewro.controller;

import com.reviewro.dto.CompanionCommentDTO;
import com.reviewro.dto.CompanionPostDTO;
import com.reviewro.entity.MemberRole;
import com.reviewro.entity.TargetType;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.CompanionService;
import com.reviewro.service.PostImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/companion")
public class CompanionController {

    private final CompanionService companionService;
    private final PostImageService postImageService;

    // ==================== [게시글 관련 핸들러] ====================

    // 글 작성 페이지 이동
    @GetMapping("/insert")
    public String companionInsertForm(@AuthenticationPrincipal MemberDetails details,
                                      Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        model.addAttribute("companionPostDTO", new CompanionPostDTO());
        return "companion/insert";
    }

    // 글 작성 처리
    @PostMapping("/insert")
    public String companionInsert(@ModelAttribute CompanionPostDTO postDto,
                                  @RequestParam(value = "images", required = false)
                                  List<MultipartFile> images,
                                  @AuthenticationPrincipal MemberDetails details,
                                  Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        try {
            Long id = companionService.insertPost(details.getMember().getMemberId(),postDto);
            postImageService.save(TargetType.COMPANION, id, images);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "companion/insert";
        }
        ra.addFlashAttribute("successMessage", "게시글을 등록했습니다.");
        return "redirect:/companion/list";
    }

    // 목록 조회 (누구나 글 볼 수 있음)
    @GetMapping("/list")
    public String companionList(Model model,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "page", defaultValue = "0") Integer page) {
        model.addAttribute("posts", companionService.list(keyword, page));
        model.addAttribute("keyword", keyword);
        log.info("dddddd");
        return "companion/list";
    }

    // 상세 조회 및 댓글 목록 조회
    @GetMapping("/read/{id}")
    public String companionRead(@PathVariable(name = "id") Long id,
                                @AuthenticationPrincipal MemberDetails details,
                                Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("postDto", companionService.readPost(id));
            model.addAttribute("images",
                    postImageService.paths(TargetType.COMPANION, id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/companion/list";
        }
        if (details != null) {
            model.addAttribute("loginMemberId", details.getMember().getMemberId());
        }
        return "companion/read";
    }

    // 수정 페이지 이동
    @GetMapping("/update/{id}")
    public String companionUpdateForm(@PathVariable(name = "id") Long id,
                                      Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("companionPostDTO", companionService.readPost(id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/companion/list";
        }
        return "companion/update";
    }

    // 수정 처리
    @PostMapping("/update")
    public String companionUpdate(@ModelAttribute CompanionPostDTO postDto,
                                  @AuthenticationPrincipal MemberDetails details,
                                  Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            companionService.updatePost(details.getMember().getMemberId(), postDto);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/companion/list";
        }
        ra.addFlashAttribute("successMessage", "게시글을 수정했습니다.");
        return "redirect:/companion/read/" + postDto.getCompanionPostId();
    }

    // 삭제 처리
    @PostMapping("/delete")
    public String companionDelete(@RequestParam Long companionPostId,
                                  @AuthenticationPrincipal MemberDetails details,
                                  RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            companionService.deletePost(companionPostId, details.getMember().getMemberId());
            postImageService.deleteAll(TargetType.COMPANION, companionPostId);
            ra.addFlashAttribute("successMessage", "게시글을 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/companion/list";
    }



}