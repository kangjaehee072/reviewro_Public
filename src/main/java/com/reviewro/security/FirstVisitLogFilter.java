package com.reviewro.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// 접속 로그는 IP 마다 접속을 시작할 때 한 줄만 남긴다.
// 같은 IP 요청이 30분 넘게 끊겼다가 다시 오면 새 접속으로 본다 (로그인 실패·잠금 표시가 붙은 요청은 따로 남는다)
@Component
public class FirstVisitLogFilter extends OncePerRequestFilter {

    private static final long GAP_MS = 30 * 60_000;

    private final Map<String, Long> lastSeen = new HashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isNewVisit(request.getRemoteAddr())) {
            request.setAttribute(LoginThrottle.LOG_ATTRIBUTE, "VISIT");
        }
        chain.doFilter(request, response);
    }

    synchronized boolean isNewVisit(String ip) {
        long now = System.currentTimeMillis();
        if (lastSeen.size() > 10_000) {
            lastSeen.values().removeIf(t -> now - t >= GAP_MS);
        }
        Long last = lastSeen.put(ip, now);
        return last == null || now - last >= GAP_MS;
    }
}
