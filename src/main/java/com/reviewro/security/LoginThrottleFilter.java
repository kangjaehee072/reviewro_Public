package com.reviewro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 로그인 요청(POST /login)을 비밀번호 확인 전에 접속 IP 별로 세어서, 막힌 IP 면 바로 돌려보낸다.
// (프록시 뒤라도 forward-headers-strategy=native 라 getRemoteAddr 가 실제 접속 IP 다)
// 스프링 빈으로 등록하지 않고 SecurityConfig 에서 필터 체인에만 끼운다 (모든 요청에 도는 걸 막기 위해)
public class LoginThrottleFilter extends OncePerRequestFilter {

    private final LoginThrottle loginThrottle;

    public LoginThrottleFilter(LoginThrottle loginThrottle) {
        this.loginThrottle = loginThrottle;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equals(request.getMethod()) || !"/login".equals(request.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }
        LoginThrottle.Result result = loginThrottle.check(request.getRemoteAddr());
        if (result == LoginThrottle.Result.ALLOW) {
            chain.doFilter(request, response);
            return;
        }
        if (result == LoginThrottle.Result.LOCKED_NOW) {
            request.setAttribute(LoginThrottle.LOG_ATTRIBUTE, "LOGIN_LOCKED");
        }
        if (LoginSuccessHandler.isAjax(request)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"로그인 시도가 너무 많아요. 30초 뒤에 다시 시도해 주세요.\"}");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/login?error=locked");
    }
}
