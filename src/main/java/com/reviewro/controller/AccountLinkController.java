package com.reviewro.controller;

import com.reviewro.config.SocialLoginRegistrations;
import com.reviewro.entity.MemberEntity;
import com.reviewro.entity.MemberProvider;
import com.reviewro.security.AccountLinkService;
import com.reviewro.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 계정 연동 화면 (/account/link). 로그인해야 들어온다 (SecurityConfig).
// 로그인 창 맨 아래 '계정 연동하기' → 아이디·비밀번호로 로그인 → 여기서 카카오 연동
@Controller
@RequiredArgsConstructor
public class AccountLinkController {

    private final AccountLinkService accountLinkService;
    private final SocialLoginRegistrations socialRegistrations;

    @GetMapping("/account/link")
    public String linkPage(@AuthenticationPrincipal MemberDetails details, Model model) {
        MemberEntity me = details.getMember();
        model.addAttribute("me", me);
        model.addAttribute("linkedLabel", me.getMemberProvider() == null ? null : AccountLinkService.label(me.getMemberProvider()));
        model.addAttribute("linkable", socialRegistrations.ids());
        accountLinkService.conflict().ifPresent(c -> {
            model.addAttribute("conflict", c);
            model.addAttribute("conflictLabel", AccountLinkService.label(c.provider()));
        });
        accountLinkService.takeResult().ifPresent(r ->
                model.addAttribute(r.error() ? "errorMessage" : "successMessage", r.message()));
        return "member/link";
    }

    // 연동 시작 → 소셜 로그인으로 보낸다. 돌아오면 LoginSuccessHandler 가 다시 이 화면으로 보낸다
    @PostMapping("/account/link/{provider}")
    public String start(@PathVariable String provider, @AuthenticationPrincipal MemberDetails details,
                        RedirectAttributes ra) {
        if (!socialRegistrations.ids().contains(provider)) {
            ra.addFlashAttribute("errorMessage", "지금은 연동할 수 없는 로그인 방식이에요.");
            return "redirect:/account/link";
        }
        try {
            accountLinkService.start(details.getMember().getMemberId(), MemberProvider.valueOf(provider.toUpperCase()));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/account/link";
        }
        return "redirect:/oauth2/authorization/" + provider;
    }

    // 소셜 전용 계정이 따로 있을 때 '그래도 연동' 확인
    @PostMapping("/account/link/confirm")
    public String confirm(@AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        try {
            MemberEntity updated = accountLinkService.confirm(details.getMember().getMemberId());
            refreshSession(details, updated);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/account/link";
    }

    @PostMapping("/account/link/cancel")
    public String cancel() {
        accountLinkService.cancel();
        return "redirect:/account/link";
    }

    @PostMapping("/account/link/unlink")
    public String unlink(@AuthenticationPrincipal MemberDetails details, RedirectAttributes ra) {
        try {
            MemberEntity updated = accountLinkService.unlink(details.getMember().getMemberId());
            refreshSession(details, updated);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/account/link";
    }

    // 세션에 올라간 회원 정보도 맞춰 둔다
    private void refreshSession(MemberDetails details, MemberEntity updated) {
        details.getMember().setMemberProvider(updated.getMemberProvider());
        details.getMember().setMemberProviderId(updated.getMemberProviderId());
        details.getMember().setMemberSocialOnly(updated.getMemberSocialOnly());
    }
}
