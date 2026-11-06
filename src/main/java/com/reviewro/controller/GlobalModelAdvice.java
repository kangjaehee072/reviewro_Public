package com.reviewro.controller;

import com.reviewro.config.SocialLoginRegistrations;
import com.reviewro.entity.MemberEntity;
import com.reviewro.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final SocialLoginRegistrations socialRegistrations;

    // 로그인 창·가입 화면에 띄울 소셜 로그인 버튼 (.env 에 키가 있는 제공자만)
    @ModelAttribute("socialProviders")
    public List<String> socialProviders() {
        return socialRegistrations.ids();
    }

    // 심사 중이라 눌리지 않게 띄울 소셜 버튼
    @ModelAttribute("socialPending")
    public List<String> socialPending() {
        return socialRegistrations.pending();
    }

    @ModelAttribute("loginMember")
    public MemberEntity loginMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberDetails details)) {
            return null;
        }
        return details.getMember();
    }
}
