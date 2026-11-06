package com.reviewro.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginThrottle loginThrottle;

    public LoginFailureHandler(LoginThrottle loginThrottle) {
        super("/login?error");
        this.loginThrottle = loginThrottle;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (loginThrottle.failedThird(request.getRemoteAddr())) {
            // 아이디는 로그 줄이 깨지지 않게 영문·숫자·일부 기호만 30자까지 남긴다
            String id = String.valueOf(request.getParameter("memberUserId")).replaceAll("[^A-Za-z0-9_.@-]", "");
            request.setAttribute(LoginThrottle.LOG_ATTRIBUTE,
                    "LOGIN_FAIL_3 id=" + id.substring(0, Math.min(id.length(), 30)));
        }
        if (LoginSuccessHandler.isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"아이디 또는 비밀번호가 올바르지 않습니다.\"}");
            return;
        }
        // 로그인 뒤 갈 곳(next)이 있었으면 다시 로그인 화면으로 돌아갈 때도 들고 간다
        String next = request.getParameter("next");
        if (LoginSuccessHandler.isSafePath(next)) {
            getRedirectStrategy().sendRedirect(request, response,
                    "/login?error&next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
            return;
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
