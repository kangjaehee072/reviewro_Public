package com.reviewro.controller;

import com.reviewro.dto.BalanceGameDTO;
import com.reviewro.entity.MemberRole;
import com.reviewro.entity.TargetType;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.BalanceGameService;
import com.reviewro.service.PostImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/balance")
public class BalanceGameController {

    private final BalanceGameService balanceGameService;
    private final PostImageService postImageService;

    @GetMapping("/insert")
    public String insert(@AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        return "/balance/insert";
    }

    @PostMapping("/insert")
    public String insertBalanceGame(@ModelAttribute("balanceGameDto") BalanceGameDTO dto,
                                    @AuthenticationPrincipal MemberDetails details,
                                    Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        try {
            balanceGameService.insert(details.getMember().getMemberId(), dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "balance/insert";
        }

        ra.addFlashAttribute("message", "밸런스 게임이 등록되었습니다.");
        return "redirect:/balance/list";
    }

    // 2. 밸런스 게임 목록 페이지 조회 (list)
    @GetMapping("/list")
    public String list(Model model,
                       @RequestParam(value = "page", defaultValue = "0") Integer page) {
        model.addAttribute("balanceGameList", balanceGameService.list(page));
        return "balance/list";
    }

    // 상세 조회
    @GetMapping("/read/{id}")
    public String read(@PathVariable("id") Long id,
                       @AuthenticationPrincipal MemberDetails details,
                       Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("balanceGameDto", balanceGameService.read(id));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/balance/list";
        }

        if (details != null) {
            model.addAttribute("loginMemberId", details.getMember().getMemberId());
        }
        return "balance/read"; // 경로에 맞게 수정
    }

    // 4. 상세 페이지에서 추가 투표하기 처리
    @PostMapping("/vote")
    public String vote(@RequestParam("balanceGameId") Long balanceGameId,
                       @RequestParam("selectedOption") String selectedOption,
                       @AuthenticationPrincipal MemberDetails details,
                       RedirectAttributes ra) {
        if (details == null) {
            return "redirect:/login";
        }
        try {
            String result = balanceGameService.vote(
                    balanceGameId, details.getMember().getMemberId(), selectedOption);
            ra.addFlashAttribute("message", result);

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());

        } catch (DataIntegrityViolationException e) {
            // 버튼을 아주 빠르게 두 번 눌러서 유니크 제약에 걸린 경우
            ra.addFlashAttribute("errorMessage", "이미 투표하셨습니다.");
        }
        return "redirect:/balance/read/" + balanceGameId;
    }


    @GetMapping("/update/{id}")
    public String updateForm(@PathVariable("id") Long id,
                             @AuthenticationPrincipal MemberDetails details,
                             Model model, RedirectAttributes ra) {
        if (details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        // 관리자 권한 체크가 필요하다면 여기서 검증하거나 서비스에서 처리할 수 있습니다.
        try {
            BalanceGameDTO balanceGameDto = balanceGameService.read(id); // 또는 기존 조회 메서드명
            model.addAttribute("balanceGameDto", balanceGameDto);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/balance/list";
        }

        return "balance/update";
    }

    @PostMapping("/update")
    public String update(@ModelAttribute("balanceGameDto") BalanceGameDTO balanceGameDto,
                         @AuthenticationPrincipal MemberDetails details,
                         Model model, RedirectAttributes ra) {
        if(details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try {
            balanceGameService.update(details.getMember().getMemberId(), balanceGameDto);
        }catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "balance/update";
        }
        ra.addFlashAttribute("successMessage", "밸런스게임 옵션을 수정했습니다.");
        return "redirect:/balance/read/" + balanceGameDto.getBalanceGameId();
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long balanceGameId,
                         @AuthenticationPrincipal MemberDetails details,
                         Model model, RedirectAttributes ra) {
        if(details == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        try{
            balanceGameService.delete(balanceGameId, details.getMember().getMemberId());
            ra.addFlashAttribute("successMessage", "게시글을 삭제했습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/balance/list";
    }



}

