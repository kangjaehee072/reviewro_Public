package com.reviewro.controller;

import com.reviewro.config.AutoMapper;
import com.reviewro.dto.MemberDTO;
import com.reviewro.entity.MemberEntity;
import com.reviewro.security.MemberDetails;
import com.reviewro.service.MemberService;
import com.reviewro.service.MyPageService;
import com.reviewro.service.WithdrawalService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MemberService memberService;
    private final MyPageService myPageService;
    private final WithdrawalService withdrawalService;

    @GetMapping("/mypage")
    public String index(@AuthenticationPrincipal MemberDetails details, Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        Long memberId = details.getMember().getMemberId();
        model.addAttribute("summary", myPageService.summary(memberId));
        model.addAttribute("nextTrip", myPageService.nextTrip(memberId));
        model.addAttribute("today", LocalDate.now());
        return "mypage/index";
    }

    @GetMapping("/mypage/edit")
    public String editForm(@AuthenticationPrincipal MemberDetails details, Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        MemberDTO dto = AutoMapper.map(details.getMember(), MemberDTO.class);
        dto.setMemberPassword(null);
        model.addAttribute("memberDto", dto);
        return "mypage/edit";
    }

    @PostMapping("/mypage/edit")
    public String edit(@ModelAttribute("memberDto") MemberDTO memberDto, @AuthenticationPrincipal MemberDetails details,
                       Model model, RedirectAttributes redirectAttributes) {
        if (details == null) {
            return "redirect:/login";
        }
        try {
            MemberEntity updated = memberService.updateProfile(details.getMember().getMemberId(), memberDto);
            // 세션에 올라간 회원 정보 갱신
            AutoMapper.copy(updated, details.getMember());
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "mypage/edit";
        }
        redirectAttributes.addFlashAttribute("successMessage", "개인정보가 변경되었습니다.");
        return "redirect:/mypage";
    }

    @GetMapping("/mypage/withdraw")
    public String withdrawForm(@AuthenticationPrincipal MemberDetails details, Model model) {
        if (details == null) {
            return "redirect:/login";
        }
        model.addAttribute("confirmText", WithdrawalService.CONFIRM_TEXT);
        return "mypage/withdraw";
    }

    @PostMapping("/mypage/withdraw")
    public String withdraw(@AuthenticationPrincipal MemberDetails details,
                           @RequestParam(value = "password", required = false) String password,
                           @RequestParam(value = "confirmText", required = false) String confirmText,
                           HttpServletRequest request, Model model, RedirectAttributes redirectAttributes)
            throws ServletException {
        if (details == null) {
            return "redirect:/login";
        }
        try {
            withdrawalService.withdraw(details.getMember().getMemberId(), password, confirmText);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("confirmText", WithdrawalService.CONFIRM_TEXT);
            return "mypage/withdraw";
        }
        // 지금 쓰던 세션도 로그아웃 (다른 기기 세션은 서비스에서 끊는다)
        request.logout();
        redirectAttributes.addFlashAttribute("successMessage", "회원 탈퇴가 완료되었어요. 그동안 이용해 주셔서 감사합니다.");
        return "redirect:/login";
    }

}
