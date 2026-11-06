package com.reviewro.config;

import com.reviewro.security.MemberDetails;
import com.reviewro.security.SocialMemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

// 소셜로 처음 온 사람은 약관에 동의해야 가입(저장)된다. 그 전까지는 동의 화면과 약관만 볼 수 있다.
// 저장 전이라 회원 번호가 없어서, 다른 화면에 들어가면 회원 번호를 쓰는 곳에서 깨진다
public class SocialSignupGate implements HandlerInterceptor {

    static final String CONSENT_PATH = "/signup/social";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof MemberDetails details)
                || !SocialMemberService.isPending(details.getMember())) {
            return true;
        }
        if (SecurityConfig.isDataRequest(request)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.sendRedirect(request.getContextPath() + CONSENT_PATH);
        }
        return false;
    }
}
