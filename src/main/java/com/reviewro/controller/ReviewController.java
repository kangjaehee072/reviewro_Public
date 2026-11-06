package com.reviewro.controller;

import com.reviewro.dto.ReviewDTO;
import com.reviewro.entity.TargetType;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.PostImageService;
import com.reviewro.service.ReactionService;
import org.springframework.data.domain.Page;
import com.reviewro.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Log4j2
public class ReviewController {

    private final ReviewService reviewService;
    private final PostImageService postImageService;
    private final ReactionService reactionService;

    // 목록
    @GetMapping("/review/list")
    public String reviewList(Model model,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "page", defaultValue = "0") Integer page) {
        Page<ReviewDTO> reviews = reviewService.list(keyword, page);
        fillLikeCounts(reviews.getContent());
        model.addAttribute("reviews", reviews);
        model.addAttribute("keyword", keyword);
        return "review/list";
    }

    // 목록에 보일 글들의 좋아요 수를 한 번에 세서 채운다
    private void fillLikeCounts(List<ReviewDTO> reviews) {
        List<Long> ids = reviews.stream().map(ReviewDTO::getReviewId).toList();
        Map<Long, Long> likes = reactionService.likeCounts(TargetType.REVIEW, ids);
        reviews.forEach(r -> r.setLikeCount(likes.getOrDefault(r.getReviewId(), 0L)));
    }

    // 글쓰기 폼
    @GetMapping("/review/insert")
    public String reviewInsertForm(@AuthenticationPrincipal MemberDetails details,
                                   Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        model.addAttribute("reviewDto", new ReviewDTO());
        return "review/insert";
    }

    // 글쓰기
    @PostMapping("/review/insert")
    public String insert(@ModelAttribute ReviewDTO dto,
                         @RequestParam(value = "images", required = false)
                         List<MultipartFile> images,
                         @AuthenticationPrincipal MemberDetails details,
                         Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            Long id = reviewService.insert(dto, details.getMember().getMemberId());
            postImageService.save(TargetType.REVIEW, id, images);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "review/insert"; // 검증 실패 시 쓴 내용이 유지된 채 폼을 다시 그린다
        }
        ra.addFlashAttribute("successMessage", "리뷰를 등록했습니다.");
        return "redirect:/review/list";
    }

    // 상세
    @GetMapping("/review/read/{id}")
    public String reviewRead(@PathVariable(name = "id") Long id,
                             @AuthenticationPrincipal MemberDetails details,
                             Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("reviewDto", reviewService.read(id));
            model.addAttribute("images",
                    postImageService.paths(TargetType.REVIEW, id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/review/list";
        }
        if (details != null) {
            model.addAttribute("loginMemberId", details.getMember().getMemberId());
        }
        return "review/read";
    }

    // 수정 폼
    @GetMapping("/review/update/{id}")
    public String reviewUpdateForm(@PathVariable(name = "id") Long id,
                                   @AuthenticationPrincipal MemberDetails details,
                                   Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            // 본인 글이 맞는지 혹은 존재하는지 확인 겸 읽어오기
            model.addAttribute("reviewDto", reviewService.read(id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/review/list";
        }
        return "review/update";
    }

    // 수정
    @PostMapping("/review/update")
    public String update(@ModelAttribute("reviewDto") ReviewDTO reviewDto,
                         @AuthenticationPrincipal MemberDetails details,
                         Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        log.info(reviewDto + " update attempt");
        try {
            reviewService.update(details.getMember().getMemberId(), reviewDto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "review/update"; // 수정 실패 시 목록으로 튕기지 않고 작성 중이던 내용 유지
        }
        ra.addFlashAttribute("successMessage", "리뷰를 수정했습니다.");
        return "redirect:/review/read/" + reviewDto.getReviewId();
    }

    // 삭제
    @PostMapping("/review/delete")
    public String delete(@RequestParam Long reviewId,
                         @AuthenticationPrincipal MemberDetails details,
                         RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            reviewService.delete(reviewId, details.getMember().getMemberId());
            ra.addFlashAttribute("successMessage", "리뷰를 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/review/list";
    }
}