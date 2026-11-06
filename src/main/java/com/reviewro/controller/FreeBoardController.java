package com.reviewro.controller;

import com.reviewro.dto.FreeBoardDTO;
import com.reviewro.entity.TargetType;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.FreeBoardService;
import com.reviewro.service.PostImageService;
import com.reviewro.service.ReactionService;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class FreeBoardController {

    private final FreeBoardService freeBoardService;
    private final PostImageService postImageService;
    private final ReactionService reactionService;

    // 목록
    @GetMapping("/freeboard/list")
    public String list(Model model,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") Integer page) {
        Page<FreeBoardDTO> freeBoard = freeBoardService.list(keyword, page);
        fillLikeCounts(freeBoard.getContent());
        model.addAttribute("freeBoard", freeBoard);
        model.addAttribute("keyword", keyword);
        return "freeboard/list";
    }

    // 목록에 보일 글들의 좋아요 수를 한 번에 세서 채운다
    private void fillLikeCounts(List<FreeBoardDTO> posts) {
        List<Long> ids = posts.stream().map(FreeBoardDTO::getFreeBoardId).toList();
        Map<Long, Long> likes = reactionService.likeCounts(TargetType.FREE_BOARD, ids);
        posts.forEach(p -> p.setLikeCount(likes.getOrDefault(p.getFreeBoardId(), 0L)));
    }

    // 글쓰기 폼
    @GetMapping("/freeboard/insert")
    public String insertForm(@AuthenticationPrincipal MemberDetails details,
                             Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        model.addAttribute("freeBoardDto", new FreeBoardDTO());
        return "freeboard/insert";
    }

    // 글쓰기
    @PostMapping("/freeboard/insert")
    public String insert(@ModelAttribute("freeBoardDto") FreeBoardDTO freeBoardDto,
                         @RequestParam(value = "images", required = false)
                         List<MultipartFile> images,
                         @AuthenticationPrincipal MemberDetails details,
                         Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            Long id = freeBoardService.insert(details.getMember().getMemberId(), freeBoardDto);
            postImageService.save(TargetType.FREE_BOARD, id, images);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "freeboard/insert";          // 쓴 내용이 남는다
        }
        ra.addFlashAttribute("successMessage", "게시글을 등록했습니다.");
        return "redirect:/freeboard/list";
    }

    // 상세
    @GetMapping("/freeboard/read/{id}")
    public String read(@PathVariable("id") Long id,
                       @AuthenticationPrincipal MemberDetails details,
                       Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("freeBoardDto", freeBoardService.read(id));
            model.addAttribute("images",
                    postImageService.paths(TargetType.FREE_BOARD, id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/freeboard/list";  // 머물 화면이 없으니 목록으로
        }
        if (details != null) {
            model.addAttribute("loginMemberId", details.getMember().getMemberId());
        }
        return "freeboard/read";
    }

    // 수정 폼
    @GetMapping("/freeboard/update/{id}")
    public String updateForm(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("freeBoardDto", freeBoardService.read(id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/freeboard/list";
        }
        return "freeboard/update";
    }

    // 수정
    @PostMapping("/freeboard/update")
    public String update(@ModelAttribute("freeBoardDto") FreeBoardDTO freeBoardDto,
                         @AuthenticationPrincipal MemberDetails details,
                         Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            freeBoardService.update(details.getMember().getMemberId(), freeBoardDto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "freeboard/update";          // 쓴 내용이 남는다
        }
        ra.addFlashAttribute("successMessage", "게시글을 수정했습니다.");
        return "redirect:/freeboard/read/" + freeBoardDto.getFreeBoardId();
    }

    // 삭제
    @PostMapping("/freeboard/delete")
    public String delete(@RequestParam Long freeBoardId,
                         @AuthenticationPrincipal MemberDetails details,
                         RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            freeBoardService.delete(freeBoardId, details.getMember().getMemberId());
            ra.addFlashAttribute("successMessage", "게시글을 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/freeboard/list";
    }


}
