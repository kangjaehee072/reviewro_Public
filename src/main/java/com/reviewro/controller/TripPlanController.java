package com.reviewro.controller;

import com.reviewro.dto.TripPlanDTO;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.TripPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor

public class TripPlanController {

    private final TripPlanService tripPlanService;

    @GetMapping("/tripplan/insert")
    public String tripPlanInsert(@AuthenticationPrincipal MemberDetails details,
                                 Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        // 등록 POST(@ModelAttribute("tripPlanDto"))·수정 화면과 같은 이름으로 맞춘다. 폼은 이 이름 하나만 본다
        model.addAttribute("tripPlanDto", new TripPlanDTO());
        return "tripplan/insert";
    }

    @PostMapping("/tripplan/insert")
    public String insert(@ModelAttribute("tripPlanDto") TripPlanDTO tripPlanDto,
                         @AuthenticationPrincipal MemberDetails details,
                         Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            tripPlanService.insert(details.getMember().getMemberId(), tripPlanDto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "tripplan/insert";
        }
        ra.addFlashAttribute("successMessage", "게시글을 등록했습니다.");
        return "redirect:/tripplan/list";
    }

    @GetMapping("/tripplan/list")
    public String tripPlanList(Model model,
                               @AuthenticationPrincipal MemberDetails details,
                               @RequestParam(value = "page", defaultValue = "0") Integer page) {
        model.addAttribute("tripPlan", tripPlanService.list(page, viewerId(details)));
        return "tripplan/list";
    }

    @GetMapping("/tripplan/read/{id}")
    public String tripPlanRead(@PathVariable("id") Long id,
                               @AuthenticationPrincipal MemberDetails details,
                               Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("tripPlanDto", tripPlanService.read(id, viewerId(details)));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());

            return "redirect:/tripplan/list";
        }
        if (details != null) {
            model.addAttribute("loginMemberId", details.getMember().getMemberId());
        }
        return "tripplan/read";
    }

    @GetMapping("/tripplan/update/{id}")
    public String tripPlanUpdate(@PathVariable("id") Long id,
                                 @AuthenticationPrincipal MemberDetails details,
                                 Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            TripPlanDTO dto = tripPlanService.read(id, viewerId(details));
            if (!details.getMember().getMemberId().equals(dto.getMemberId())) {
                throw new IllegalArgumentException("본인이 쓴 글만 수정하거나 삭제할 수 있습니다.");
            }
            model.addAttribute("tripPlanDto", dto);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tripplan/list";
        }
        return "tripplan/update";
    }

    @PostMapping("/tripplan/update")
    public String update(@ModelAttribute("tripPlanDto") TripPlanDTO tripPlanDto,
                         @AuthenticationPrincipal MemberDetails details,
                         Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            tripPlanService.update(details.getMember().getMemberId(), tripPlanDto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "tripplan/update";
        }
        ra.addFlashAttribute("successMessage", "게시글을 수정했습니다.");
        return "redirect:/tripplan/read/" + tripPlanDto.getTripplanId();
    }

    @PostMapping("/tripplan/delete")
    public String delete(@RequestParam Long tripPlanId,
                         @AuthenticationPrincipal MemberDetails details,
                         RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            tripPlanService.delete(tripPlanId, details.getMember().getMemberId());
            ra.addFlashAttribute("successMessage", "게시글을 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tripplan/list";
    }

    // 퍼가기 -> 장소 페이지 '여행 계획 짜기'의 내 여행계획으로 저장
    @PostMapping("/tripplan/fork/{id}")
    public String forkTripPlan(@PathVariable(name = "id") Long id,
                               @RequestParam("title") String title,
                               @AuthenticationPrincipal MemberDetails details,
                               RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            tripPlanService.fork(id, details.getMember().getMemberId(), title);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tripplan/read/" + id;
        }
        return "redirect:/location#plans";
    }

    private Long viewerId(MemberDetails details) {
        return details == null ? null : details.getMember().getMemberId();
    }
}