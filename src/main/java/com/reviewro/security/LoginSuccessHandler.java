package com.reviewro.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AccountLinkService accountLinkService;

    public LoginSuccessHandler(AccountLinkService accountLinkService) {
        this.accountLinkService = accountLinkService;
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":true}");
            return;
        }
        // 계정 연동하러 소셜 로그인을 다녀왔으면 결과를 보여줄 연동 화면으로
        if (accountLinkService.hasPendingScreen()) {
            getRedirectStrategy().sendRedirect(request, response, "/account/link");
            return;
        }
        // 로그인 화면에 next 가 붙어 왔으면 (예: 계정 연동하기 → 로그인) 그리로. 우리 사이트 안 주소만
        String next = request.getParameter("next");
        if (isSafePath(next)) {
            getRedirectStrategy().sendRedirect(request, response, next);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    static boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    // 다른 사이트로 보내는 데 쓰이지 않게 '/' 로 시작하는 우리 경로만 받는다 (//evil.com, /\evil.com 막음)
    static boolean isSafePath(String path) {
        return path != null && path.startsWith("/") && !path.startsWith("//")
                && !path.contains("\\") && !path.contains("://");
    }
}
