package com.reviewro.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;

// 자동 로그인 (로그인 화면의 '자동 로그인' 체크).
// 체크한 기기(브라우저)에만 쿠키가 생기고, 로그아웃하면 그 기기의 토큰만 지운다
// (기본 동작은 그 회원의 모든 기기 토큰을 지워서 다른 기기 자동 로그인까지 풀린다)
public class DeviceRememberMeServices extends PersistentTokenBasedRememberMeServices {

    public static final String PARAMETER = "remember-me";

    // 소셜 로그인은 카카오 등을 다녀오는 사이 체크 값이 사라져서, 떠날 때 세션에 적어 두고 돌아와서 본다
    private static final String SOCIAL_FLAG = DeviceRememberMeServices.class.getName() + ".social";

    private static final int VALIDITY_SECONDS = 30 * 24 * 60 * 60;   // 30일

    private final RememberMeTokenStore store;

    public DeviceRememberMeServices(String key, UserDetailsService userDetailsService, RememberMeTokenStore store,
                                    boolean secureCookie) {
        super(key, userDetailsService, store);
        this.store = store;
        setParameter(PARAMETER);
        setTokenValiditySeconds(VALIDITY_SECONDS);
        if (secureCookie) {
            setUseSecureCookie(true);
        }
    }

    // 소셜 로그인 버튼을 누를 때 (/oauth2/authorization/..?remember-me=true) 체크 여부를 적어 둔다
    public static void rememberSocialChoice(HttpServletRequest request) {
        if ("true".equals(request.getParameter(PARAMETER))) {
            request.getSession().setAttribute(SOCIAL_FLAG, Boolean.TRUE);
        } else if (request.getSession(false) != null) {
            // 지난번에 체크하고 갔다가 실패한 흔적이 남아 있으면 지운다
            request.getSession(false).removeAttribute(SOCIAL_FLAG);
        }
    }

    @Override
    protected boolean rememberMeRequested(HttpServletRequest request, String parameter) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SOCIAL_FLAG) != null) {
            session.removeAttribute(SOCIAL_FLAG);
            return true;
        }
        return super.rememberMeRequested(request, parameter);
    }

    // 토큰은 쓸 때마다 바뀐다. 브라우저를 다시 열며 탭 여러 개가 거의 동시에 오면 뒤의 요청은 방금 바뀌기 전 토큰을 들고 와서
    // 기본 동작으로는 '도용' 으로 보고 모든 기기 자동 로그인을 지운 뒤 오류 화면을 띄운다. 바뀐 지 몇 초 안이면 같은 기기로 본다
    private static final long CONCURRENT_GRACE_MS = 10_000;

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request,
                                                 HttpServletResponse response) {
        if (cookieTokens.length == 2) {
            PersistentRememberMeToken saved = store.getTokenForSeries(cookieTokens[0]);
            if (saved != null && !saved.getTokenValue().equals(cookieTokens[1])
                    && System.currentTimeMillis() - saved.getDate().getTime() < CONCURRENT_GRACE_MS) {
                // 토큰은 그대로 두고 이 요청만 로그인시킨다 (새 토큰 쿠키는 먼저 온 요청이 이미 받았다)
                return getUserDetailsService().loadUserByUsername(saved.getUsername());
            }
        }
        try {
            return super.processAutoLoginCookie(cookieTokens, request, response);
        } catch (CookieTheftException e) {
            // 진짜 도용 의심: 그 회원의 모든 기기 토큰은 이미 지워졌다. 오류 화면 대신 로그인 안 된 상태로 보여준다
            logger.warn("자동 로그인 쿠키 도용 의심: " + maskSeries(cookieTokens[0]));
            throw new RememberMeAuthenticationException(e.getMessage());
        }
    }

    private static String maskSeries(String series) {
        return "series=" + series.substring(0, Math.min(6, series.length())) + "…";
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String cookie = extractRememberMeCookie(request);
        if (cookie != null) {
            try {
                String[] parts = decodeCookie(cookie);
                if (parts.length == 2) {
                    store.removeSeries(parts[0]);
                }
            } catch (InvalidCookieException ignored) {
                // 망가진 쿠키는 아래에서 지우기만 한다
            }
        }
        cancelCookie(request, response);
    }
}
